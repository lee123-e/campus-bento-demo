package com.neuedu.reggie.controller.admin;

import com.neuedu.reggie.dto.order.AdminOrderActionRequest;
import com.neuedu.reggie.dto.order.AdminOrderPageResponse;
import com.neuedu.reggie.dto.order.OrderDetailResponse;
import com.neuedu.reggie.dto.order.OrderSummaryResponse;
import com.neuedu.reggie.service.AdminOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orders;

    public AdminOrderController(AdminOrderService orders) {
        this.orders = orders;
    }

    @GetMapping
    public AdminOrderPageResponse list(
            Principal employee,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size,
            @RequestParam(required = false) @Size(max = 40) String number,
            @RequestParam(required = false) @Min(1) @Max(5) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return orders.list(employee.getName(), page, size, number, status, startDate, endDate);
    }

    @GetMapping("/{id}")
    public OrderDetailResponse detail(Principal employee, @PathVariable @Min(1) long id) {
        return orders.detail(employee.getName(), id);
    }

    @PutMapping("/{id}/status")
    public OrderSummaryResponse updateStatus(Principal employee, @PathVariable @Min(1) long id,
                                             @Valid @RequestBody AdminOrderActionRequest request) {
        return orders.advance(employee.getName(), id, request.action());
    }
}
