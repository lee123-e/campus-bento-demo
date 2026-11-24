package com.neuedu.reggie.dto.customer;

import java.math.BigDecimal;

public record OrderableProduct(long id, String name, BigDecimal price) {
}
