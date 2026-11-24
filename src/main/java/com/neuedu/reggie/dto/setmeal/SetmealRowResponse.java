package com.neuedu.reggie.dto.setmeal;

import com.neuedu.reggie.entity.Setmeal;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SetmealRowResponse(long id, String name, long categoryId, String categoryName, BigDecimal price,
                                 boolean onSale, LocalDateTime updatedAt) {

    public static SetmealRowResponse from(Setmeal setmeal, String categoryName) {
        return new SetmealRowResponse(setmeal.getId(), setmeal.getName(), setmeal.getCategoryId(), categoryName,
                setmeal.getPrice(), setmeal.getOnSale(), setmeal.getUpdatedAt());
    }
}
