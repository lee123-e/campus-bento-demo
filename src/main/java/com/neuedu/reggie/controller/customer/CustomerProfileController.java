package com.neuedu.reggie.controller.customer;

import com.neuedu.reggie.dto.customer.CustomerProfileRequest;
import com.neuedu.reggie.dto.customer.CustomerSessionResponse;
import com.neuedu.reggie.security.CustomerPrincipal;
import com.neuedu.reggie.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/profile")
public class CustomerProfileController {

    private final CustomerService customers;

    public CustomerProfileController(CustomerService customers) {
        this.customers = customers;
    }

    @PutMapping
    public CustomerSessionResponse update(@AuthenticationPrincipal CustomerPrincipal customer,
                                          @Valid @RequestBody CustomerProfileRequest request) {
        return CustomerSessionResponse.from(customers.updateNickname(customer.id(), request.nickname()));
    }
}
