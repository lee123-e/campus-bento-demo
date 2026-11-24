package com.neuedu.reggie.dto.cart;

import java.math.BigDecimal;

public record CartReadRow(long id, int type, long itemId, String name,
                          String flavorsJson, int quantity, BigDecimal unitPrice) {
}
