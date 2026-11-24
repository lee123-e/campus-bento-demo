package com.neuedu.reggie.dto.order;

import java.util.List;

public record OrderPageResponse(long total, List<OrderSummaryResponse> items) {
}
