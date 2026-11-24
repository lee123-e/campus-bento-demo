package com.neuedu.reggie.dto.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlavorSelection(@NotBlank @Size(max = 40) String name, @NotBlank @Size(max = 40) String option) {
}
