package com.neuedu.reggie.dto.menu;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MenuSaleStatusRequest(
        @NotEmpty @Size(max = 100) List<@NotNull @Min(1) Long> ids,
        @NotNull Boolean onSale) {
}
