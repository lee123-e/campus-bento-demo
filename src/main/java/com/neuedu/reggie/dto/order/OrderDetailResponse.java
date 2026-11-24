package com.neuedu.reggie.dto.order;

import com.neuedu.reggie.dto.cart.FlavorSelection;
import com.neuedu.reggie.dto.menu.MealComponent;
import java.math.BigDecimal;
import java.util.List;

public record OrderDetailResponse(OrderSummaryResponse order, Delivery delivery, String note, List<Line> items) {

    public record Delivery(String contactName, String phone, String deliveryAddress) {
    }

    public record Line(int type, long itemId, String name, BigDecimal unitPrice, int quantity,
                       BigDecimal subtotal, List<FlavorSelection> flavors, List<MealComponent> mealContents) {
    }
}
