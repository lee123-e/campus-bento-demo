package com.neuedu.reggie.service;

import com.neuedu.reggie.dto.cart.CartAddRequest;
import com.neuedu.reggie.dto.cart.CartItemIssue;
import com.neuedu.reggie.dto.cart.CartItemResponse;
import com.neuedu.reggie.dto.cart.CartReadRow;
import com.neuedu.reggie.dto.cart.CartResponse;
import com.neuedu.reggie.dto.cart.FlavorSelection;
import com.neuedu.reggie.dto.customer.OrderableProduct;
import com.neuedu.reggie.dto.menu.MealComponent;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.ShoppingCartMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CustomerCartService {

    private final ShoppingCartMapper carts;
    private final CustomerService customers;
    private final CustomerCatalogService catalog;
    private final MenuFlavorService flavors;
    private final JsonMapper json;

    public CustomerCartService(ShoppingCartMapper carts, CustomerService customers, CustomerCatalogService catalog,
                               MenuFlavorService flavors, JsonMapper json) {
        this.carts = carts;
        this.customers = customers;
        this.catalog = catalog;
        this.flavors = flavors;
        this.json = json;
    }

    public CartResponse get(long customerId) {
        return quoteRows(carts.findRows(customerId));
    }

    private CartResponse quote(List<CartItemResponse> items) {
        int quantity = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal total = items.stream().filter(CartItemResponse::available)
                .map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(items, quantity, total, sha256(json.writeValueAsString(items)));
    }

    @Transactional
    public CartResponse add(long customerId, CartAddRequest input) {
        customers.lockForWrite(customerId);
        OrderableProduct product = catalog.lockOrderable(input.type(), input.itemId());
        String selections = flavors.encode(flavors.validate(input.type(), input.itemId(), input.flavors()));
        String key = sha256(selections);
        var existing = carts.findVariant(customerId, input.type(), input.itemId(), key);
        if (existing.isPresent()) {
            CartReadRow row = existing.get();
            if (row.quantity() == 99) {
                throw conflict("同一项商品最多添加 99 份。");
            }
            carts.updateQuantity(customerId, row.id(), row.quantity() + 1);
        } else {
            if (carts.count(customerId) >= 50) {
                throw conflict("购物车最多保存 50 项商品。");
            }
            carts.insert(customerId, input.type(), input.itemId(), product.name(), selections, key);
        }
        return get(customerId);
    }

    @Transactional
    public CartResponse setQuantity(long customerId, long id, int quantity) {
        customers.lockForWrite(customerId);
        var current = carts.findRow(customerId, id).orElseThrow(CustomerCartService::missing);
        if (quantity == 0) {
            carts.deleteItem(customerId, id);
        } else {
            if (quantity > current.quantity()) {
                catalog.lockOrderable(current.type(), current.itemId());
                try {
                    flavors.validate(current.type(), current.itemId(), flavors.decode(current.flavorsJson()));
                } catch (BusinessException failure) {
                    throw flavorChanged(current.name());
                }
            }
            carts.updateQuantity(customerId, id, quantity);
        }
        return get(customerId);
    }

    @Transactional
    public CartResponse replaceFlavors(long customerId, long id, List<FlavorSelection> requested) {
        customers.lockForWrite(customerId);
        CartReadRow current = carts.findRow(customerId, id).orElseThrow(CustomerCartService::missing);
        if (current.type() != 1) {
            throw conflict("这项商品无需选择口味。");
        }
        OrderableProduct product = catalog.lockOrderable(current.type(), current.itemId());
        String stored = flavors.encode(flavors.validate(current.type(), current.itemId(), requested));
        String key = sha256(stored);
        var matched = carts.findVariant(customerId, current.type(), current.itemId(), key);
        if (matched.isEmpty()) {
            carts.replaceVariant(customerId, id, stored, key, product.name());
        } else if (matched.get().id() != id) {
            CartReadRow existing = matched.get();
            int quantity = existing.quantity() + current.quantity();
            if (quantity > 99) {
                throw conflict("合并后超过同一项商品的 99 份上限，请先减少数量。");
            }
            carts.updateQuantity(customerId, existing.id(), quantity);
            carts.deleteItem(customerId, id);
        }
        return get(customerId);
    }

    @Transactional
    public CartResponse remove(long customerId, long id) {
        customers.lockForWrite(customerId);
        if (carts.deleteItem(customerId, id) == 0) {
            throw missing();
        }
        return get(customerId);
    }

    @Transactional
    public CartResponse clear(long customerId) {
        customers.lockForWrite(customerId);
        carts.clear(customerId);
        return get(customerId);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public CartResponse confirmQuote(long customerId, String quote) {
        var requested = carts.findRows(customerId);
        if (requested.isEmpty()) {
            throw conflict("购物车为空，请先添加商品。");
        }
        List<ProductKey> keys = requested.stream()
                .map(row -> new ProductKey(row.type(), row.itemId()))
                .distinct()
                .sorted(Comparator.comparingInt(ProductKey::type).reversed().thenComparingLong(ProductKey::id))
                .toList();
        Map<ProductKey, OrderableProduct> products = new HashMap<>();
        for (ProductKey key : keys) {
            products.put(key, catalog.lockOrderable(key.type(), key.id()));
        }
        // Quote from the locked rows, since the initial cart query may be cached in this session.
        CartResponse current = quoteRows(requested.stream().map(row -> {
            OrderableProduct product = products.get(new ProductKey(row.type(), row.itemId()));
            return new CartReadRow(row.id(), row.type(), row.itemId(), product.name(),
                    row.flavorsJson(), row.quantity(), product.price());
        }).toList());
        for (CartItemResponse item : current.items()) {
            if (item.issue() == CartItemIssue.FLAVOR_CHANGED) {
                throw flavorChanged(item.name());
            }
            if (!item.available()) {
                throw conflict("购物车中有商品暂不可结算，请刷新后调整。");
            }
        }
        if (!current.quote().equals(quote)) {
            throw conflict("购物车、价格或套餐内容已变化，请刷新结算页后重新确认。");
        }
        return current;
    }

    private CartResponse quoteRows(List<CartReadRow> rows) {
        var currentGroups = flavors.findGroups(availableIds(rows, 1));
        var mealContents = catalog.mealContents(availableIds(rows, 2));
        var items = rows.stream().map(row -> {
            var choices = flavors.decode(row.flavorsJson());
            var contents = row.type() == 2 ? mealContents.getOrDefault(row.itemId(), List.of()) : List.<MealComponent>of();
            CartItemIssue issue = null;
            if (row.unitPrice() == null || (row.type() == 2 && contents.isEmpty())) {
                issue = CartItemIssue.UNAVAILABLE;
            } else if (row.type() == 1 && !flavors.matches(currentGroups.getOrDefault(row.itemId(), List.of()), choices)) {
                issue = CartItemIssue.FLAVOR_CHANGED;
            }
            BigDecimal subtotal = issue == null ? row.unitPrice().multiply(BigDecimal.valueOf(row.quantity())) : null;
            return new CartItemResponse(row.id(), row.type(), row.itemId(), row.name(), choices, contents,
                    row.quantity(), issue == null, issue, row.unitPrice(), subtotal);
        }).toList();
        return quote(items);
    }

    private static List<Long> availableIds(List<CartReadRow> rows, int type) {
        return rows.stream().filter(row -> row.type() == type && row.unitPrice() != null)
                .map(CartReadRow::itemId).distinct().toList();
    }

    private static BusinessException flavorChanged(String name) {
        return conflict("「" + name + "」的口味已变化，请返回购物车重新选择。");
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private static BusinessException conflict(String message) {
        return new BusinessException(BusinessException.Reason.RULE_CONFLICT, message);
    }

    private static BusinessException missing() {
        return new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "这项商品已从购物车移除。");
    }

    private record ProductKey(int type, long id) {
    }
}
