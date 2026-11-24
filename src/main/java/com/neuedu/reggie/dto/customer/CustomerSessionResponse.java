package com.neuedu.reggie.dto.customer;

import com.neuedu.reggie.entity.Customer;

public record CustomerSessionResponse(long id, String phone, String nickname) {

    public static CustomerSessionResponse from(Customer customer) {
        return new CustomerSessionResponse(customer.id(), customer.phone(), customer.nickname());
    }
}
