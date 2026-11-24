package com.neuedu.reggie.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull @Min(1) Long addressId,
        @Size(max = 200) String note,
        @NotNull UUID requestId,
        @NotNull @Pattern(regexp = "[a-f0-9]{64}") String quote) {
}
