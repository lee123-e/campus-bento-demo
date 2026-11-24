package com.neuedu.reggie.controller.customer;

import com.neuedu.reggie.dto.order.OrderDetailResponse;
import com.neuedu.reggie.dto.order.OrderPageResponse;
import com.neuedu.reggie.dto.order.OrderSummaryResponse;
import com.neuedu.reggie.dto.order.PlaceOrderRequest;
import com.neuedu.reggie.security.CustomerPrincipal;
import com.neuedu.reggie.service.CustomerOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/orders")
public class CustomerOrderController {

    private final CustomerOrderService orders;

    public CustomerOrderController(CustomerOrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public OrderPageResponse list(@AuthenticationPrincipal CustomerPrincipal customer,
                                  @RequestParam(defaultValue = "1") @Min(1) int page,
                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return orders.list(customer.id(), page, size);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse detail(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable @Min(1) long id) {
        return orders.detail(customer.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderSummaryResponse place(@AuthenticationPrincipal CustomerPrincipal customer,
                                      @Valid @RequestBody PlaceOrderRequest request) {
        return orders.place(customer.id(), request);
    }

    @PostMapping("/{id}/demo-payment")
    public OrderSummaryResponse pay(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable @Min(1) long id) {
        return orders.simulatePayment(customer.id(), id);
    }
}
