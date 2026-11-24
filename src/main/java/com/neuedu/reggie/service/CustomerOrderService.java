package com.neuedu.reggie.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.neuedu.reggie.dto.order.OrderDetailResponse;
import com.neuedu.reggie.dto.order.OrderPageResponse;
import com.neuedu.reggie.dto.order.OrderSummaryResponse;
import com.neuedu.reggie.dto.order.PlaceOrderRequest;
import com.neuedu.reggie.entity.CustomerOrder;
import com.neuedu.reggie.entity.OrderLine;
import com.neuedu.reggie.entity.OrderStatus;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.CustomerOrderMapper;
import com.neuedu.reggie.mapper.OrderLineMapper;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class CustomerOrderService {

    private final CustomerOrderMapper orders;
    private final OrderLineMapper lines;
    private final OrderDetailReader details;
    private final CustomerService customers;
    private final CustomerAddressService addresses;
    private final CustomerCartService cart;
    private final MenuFlavorService flavors;
    private final JsonMapper json;
    private final boolean demoPayment;

    public CustomerOrderService(CustomerOrderMapper orders, OrderLineMapper lines, CustomerService customers,
                                CustomerAddressService addresses, CustomerCartService cart, MenuFlavorService flavors,
                                JsonMapper json, OrderDetailReader details,
                                @Value("${reggie.order.demo-payment}") boolean demoPayment) {
        this.orders = orders;
        this.lines = lines;
        this.details = details;
        this.customers = customers;
        this.addresses = addresses;
        this.cart = cart;
        this.flavors = flavors;
        this.json = json;
        this.demoPayment = demoPayment;
    }

    @Transactional
    public OrderSummaryResponse place(long customerId, PlaceOrderRequest request) {
        customers.lockForWrite(customerId);
        String requestId = request.requestId().toString();
        CustomerOrder existing = orders.selectOne(owned(customerId).eq(CustomerOrder::getRequestId, requestId));
        if (existing != null) {
            return OrderSummaryResponse.from(existing);
        }

        var address = addresses.getOwned(customerId, request.addressId());
        var confirmed = cart.confirmQuote(customerId, request.quote());
        var order = new CustomerOrder(customerId, orderNumber(), requestId, confirmed.total(),
                address.contactName(), address.phone(), address.deliveryAddress(),
                request.note() == null ? "" : request.note().strip(), now());
        orders.insert(order);

        var ordered = confirmed.items().stream()
                .map(item -> new OrderLine(item.type(), item.itemId(), item.name(), item.unitPrice(),
                        item.quantity(), flavors.encode(item.flavors()), json.writeValueAsString(item.mealContents())))
                .toList();
        lines.insertForOrder(order.getId(), ordered);
        cart.clear(customerId);
        return OrderSummaryResponse.from(order);
    }

    public OrderPageResponse list(long customerId, int page, int size) {
        Page<CustomerOrder> result = orders.selectPage(new Page<>(page, size), owned(customerId)
                .orderByDesc(CustomerOrder::getCreatedAt, CustomerOrder::getId));
        return new OrderPageResponse(result.getTotal(), result.getRecords().stream().map(OrderSummaryResponse::from).toList());
    }

    public OrderDetailResponse detail(long customerId, long id) {
        return details.read(requireOwned(customerId, id));
    }

    @Transactional
    public OrderSummaryResponse simulatePayment(long customerId, long id) {
        if (!demoPayment) {
            throw new BusinessException(BusinessException.Reason.SERVICE_UNAVAILABLE, "模拟支付暂未开启。");
        }
        customers.lockForWrite(customerId);
        CustomerOrder order = requireOwned(customerId, id);
        if (order.getPaidAt() != null) {
            return OrderSummaryResponse.from(order);
        }
        int updated = orders.update(null, Wrappers.<CustomerOrder>lambdaUpdate()
                .eq(CustomerOrder::getCustomerId, customerId)
                .eq(CustomerOrder::getId, id)
                .eq(CustomerOrder::getStatus, OrderStatus.PENDING_PAYMENT.code())
                .set(CustomerOrder::getStatus, OrderStatus.AWAITING_DISPATCH.code())
                .set(CustomerOrder::getPaidAt, now()));
        if (updated != 1) {
            throw new BusinessException(BusinessException.Reason.RULE_CONFLICT, "此订单当前不能支付。");
        }
        return OrderSummaryResponse.from(requireOwned(customerId, id));
    }

    private CustomerOrder requireOwned(long customerId, long id) {
        CustomerOrder order = orders.selectOne(owned(customerId).eq(CustomerOrder::getId, id));
        if (order == null) {
            throw new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "这笔订单不存在。");
        }
        return order;
    }

    private static LambdaQueryWrapper<CustomerOrder> owned(long customerId) {
        return Wrappers.<CustomerOrder>lambdaQuery().eq(CustomerOrder::getCustomerId, customerId);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }

    private static String orderNumber() {
        return "CB" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }
}
