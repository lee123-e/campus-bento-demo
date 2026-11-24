package com.neuedu.reggie.dto.order;

import com.neuedu.reggie.entity.CustomerOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryResponse(long id, String number, int status, BigDecimal total,
                                   LocalDateTime createdAt, LocalDateTime paidAt,
                                   LocalDateTime dispatchedAt, LocalDateTime completedAt) {

    public static OrderSummaryResponse from(CustomerOrder order) {
        return new OrderSummaryResponse(order.getId(), order.getOrderNumber(), order.getStatus(), order.getTotal(),
                order.getCreatedAt(), order.getPaidAt(), order.getDispatchedAt(), order.getCompletedAt());
    }
}
