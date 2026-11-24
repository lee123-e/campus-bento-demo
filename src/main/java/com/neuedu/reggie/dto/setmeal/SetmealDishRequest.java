package com.neuedu.reggie.dto.setmeal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetmealDishRequest(
        @NotNull @Min(1) Long dishId,
        @NotNull @Min(1) @Max(99) Integer quantity) {
}
