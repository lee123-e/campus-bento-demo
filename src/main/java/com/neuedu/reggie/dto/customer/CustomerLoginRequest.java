package com.neuedu.reggie.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerLoginRequest(
        @NotBlank @Pattern(regexp = "1[3-9][0-9]{9}") String phone,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String code) {
}
