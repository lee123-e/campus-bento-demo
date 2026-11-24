package com.neuedu.reggie.dto.setmeal;

import java.math.BigDecimal;

public record SetmealDishResponse(long dishId, String name, BigDecimal price, boolean onSale, int quantity) {
}
