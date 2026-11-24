package com.neuedu.reggie.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerCodeRequest(@NotBlank @Pattern(regexp = "1[3-9][0-9]{9}") String phone) {
}
