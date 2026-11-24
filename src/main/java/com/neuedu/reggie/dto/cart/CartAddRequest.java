package com.neuedu.reggie.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CartAddRequest(
        @NotNull @Min(1) @Max(2) Integer type,
        @NotNull @Min(1) Long itemId,
        @Size(max = 12) List<@Valid @NotNull FlavorSelection> flavors) {
}
