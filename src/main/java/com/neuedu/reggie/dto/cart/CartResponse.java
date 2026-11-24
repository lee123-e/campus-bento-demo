package com.neuedu.reggie.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemResponse> items, int quantity, BigDecimal total, String quote) {
}
