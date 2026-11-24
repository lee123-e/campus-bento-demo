package com.neuedu.reggie.dto.category;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotNull @Min(1) @Max(2) Integer type,
        @NotBlank @Size(max = 40) String name,
        @NotNull @Min(0) Integer sort) {
}
