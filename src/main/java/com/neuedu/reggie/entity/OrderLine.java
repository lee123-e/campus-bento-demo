package com.neuedu.reggie.entity;

import java.math.BigDecimal;

public record OrderLine(int type, long itemId, String name, BigDecimal unitPrice, int quantity,
                        String flavorsJson, String mealContentsJson) {
}
