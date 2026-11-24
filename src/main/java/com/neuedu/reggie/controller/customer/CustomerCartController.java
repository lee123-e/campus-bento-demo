package com.neuedu.reggie.controller.customer;

import com.neuedu.reggie.dto.cart.CartAddRequest;
import com.neuedu.reggie.dto.cart.CartQuantityRequest;
import com.neuedu.reggie.dto.cart.CartReplaceFlavorsRequest;
import com.neuedu.reggie.dto.cart.CartResponse;
import com.neuedu.reggie.security.CustomerPrincipal;
import com.neuedu.reggie.service.CustomerCartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/cart")
public class CustomerCartController {

    private final CustomerCartService cart;

    public CustomerCartController(CustomerCartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal CustomerPrincipal customer) {
        return cart.get(customer.id());
    }

    @PostMapping("/items")
    public CartResponse add(@AuthenticationPrincipal CustomerPrincipal customer, @Valid @RequestBody CartAddRequest input) {
        return cart.add(customer.id(), input);
    }

    @PutMapping("/items/{id}")
    public CartResponse setQuantity(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable @Min(1) long id,
                                    @Valid @RequestBody CartQuantityRequest input) {
        return cart.setQuantity(customer.id(), id, input.quantity());
    }

    @PutMapping("/items/{id}/flavors")
    public CartResponse replaceFlavors(@AuthenticationPrincipal CustomerPrincipal customer,
                                        @PathVariable @Min(1) long id,
                                        @Valid @RequestBody CartReplaceFlavorsRequest input) {
        return cart.replaceFlavors(customer.id(), id, input.flavors());
    }

    @DeleteMapping("/items/{id}")
    public CartResponse remove(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable @Min(1) long id) {
        return cart.remove(customer.id(), id);
    }

    @DeleteMapping
    public CartResponse clear(@AuthenticationPrincipal CustomerPrincipal customer) {
        return cart.clear(customer.id());
    }
}
