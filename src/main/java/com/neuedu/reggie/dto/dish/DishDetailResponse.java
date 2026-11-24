package com.neuedu.reggie.dto.dish;

import java.util.List;

public record DishDetailResponse(DishRowResponse dish, String description, List<DishFlavorGroup> flavors) {
}
