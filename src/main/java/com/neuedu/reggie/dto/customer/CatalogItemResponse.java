package com.neuedu.reggie.dto.customer;

import java.math.BigDecimal;

public record CatalogItemResponse(long id, int type, String name, String description, BigDecimal price) {
}
