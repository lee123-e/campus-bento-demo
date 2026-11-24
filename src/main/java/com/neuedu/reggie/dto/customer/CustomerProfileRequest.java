package com.neuedu.reggie.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerProfileRequest(@NotBlank @Size(max = 24) String nickname) {
}
