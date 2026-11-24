package com.neuedu.reggie.service;

import com.neuedu.reggie.dto.menu.MealComponent;
import com.neuedu.reggie.dto.order.OrderDetailResponse;
import com.neuedu.reggie.dto.order.OrderSummaryResponse;
import com.neuedu.reggie.entity.CustomerOrder;
import com.neuedu.reggie.mapper.OrderLineMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderDetailReader {

    private static final TypeReference<List<MealComponent>> MEAL_SNAPSHOT = new TypeReference<>() {};

    private final OrderLineMapper lines;
    private final MenuFlavorService flavors;
    private final JsonMapper json;

    public OrderDetailReader(OrderLineMapper lines, MenuFlavorService flavors, JsonMapper json) {
        this.lines = lines;
        this.flavors = flavors;
        this.json = json;
    }

    public OrderDetailResponse read(CustomerOrder order) {
        var items = lines.findByOrder(order.getId()).stream()
                .map(item -> new OrderDetailResponse.Line(item.type(), item.itemId(), item.name(),
                        item.unitPrice(), item.quantity(), item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())),
                        flavors.decode(item.flavorsJson()), json.readValue(item.mealContentsJson(), MEAL_SNAPSHOT)))
                .toList();
        return new OrderDetailResponse(OrderSummaryResponse.from(order),
                new OrderDetailResponse.Delivery(order.getContactName(), order.getPhone(), order.getDeliveryAddress()),
                order.getNote(), items);
    }
}
