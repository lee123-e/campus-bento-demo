package com.neuedu.reggie.dto.cart;

import com.neuedu.reggie.dto.menu.MealComponent;
import java.math.BigDecimal;
import java.util.List;

public record CartItemResponse(long id, int type, long itemId, String name, List<FlavorSelection> flavors,
                               List<MealComponent> mealContents, int quantity, boolean available,
                               CartItemIssue issue, BigDecimal unitPrice, BigDecimal subtotal) {
}
