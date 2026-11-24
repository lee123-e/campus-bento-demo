package com.neuedu.reggie.dto.customer;

import java.math.BigDecimal;

public record CatalogRow(long categoryId, String categoryName, int type, long id,
                         String name, String description, BigDecimal price) {

    public CatalogItemResponse item() {
        return new CatalogItemResponse(id, type, name, description, price);
    }
}
