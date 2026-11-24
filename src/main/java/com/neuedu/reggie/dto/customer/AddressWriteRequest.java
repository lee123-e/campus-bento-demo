package com.neuedu.reggie.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressWriteRequest(
        @NotBlank @Size(max = 32) String contactName,
        @NotBlank @Pattern(regexp = "1[3-9][0-9]{9}") String phone,
        @NotBlank @Size(max = 200) String deliveryAddress,
        @Size(max = 12) String label) {
}
