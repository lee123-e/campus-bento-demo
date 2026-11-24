package com.neuedu.reggie.dto.dish;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record DishWriteRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull @Min(1) Long categoryId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @Size(max = 500) String description,
        @Size(max = 12) List<@NotNull @Valid DishFlavorGroup> flavors) {
}
