package com.neuedu.reggie.dto.customer;

import com.neuedu.reggie.dto.dish.DishFlavorGroup;
import com.neuedu.reggie.dto.menu.MealComponent;
import java.util.List;

public record CatalogDetailResponse(CatalogItemResponse item, List<DishFlavorGroup> flavors, List<MealComponent> contents) {
}
