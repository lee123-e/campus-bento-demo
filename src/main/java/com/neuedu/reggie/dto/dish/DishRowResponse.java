package com.neuedu.reggie.dto.dish;

import com.neuedu.reggie.entity.Dish;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DishRowResponse(long id, String name, long categoryId, String categoryName, BigDecimal price,
                              boolean onSale, LocalDateTime updatedAt) {

    public static DishRowResponse from(Dish dish, String categoryName) {
        return new DishRowResponse(dish.getId(), dish.getName(), dish.getCategoryId(), categoryName,
                dish.getPrice(), dish.getOnSale(), dish.getUpdatedAt());
    }
}
