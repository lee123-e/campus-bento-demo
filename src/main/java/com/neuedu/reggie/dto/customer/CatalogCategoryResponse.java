package com.neuedu.reggie.dto.customer;

import java.util.List;

public record CatalogCategoryResponse(long id, String name, int type, List<CatalogItemResponse> items) {
}
