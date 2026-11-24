package com.neuedu.reggie.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CartReplaceFlavorsRequest(@NotNull @Size(max = 12) List<@Valid @NotNull FlavorSelection> flavors) {
}
