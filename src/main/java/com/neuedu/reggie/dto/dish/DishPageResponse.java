package com.neuedu.reggie.dto.dish;

import java.util.List;

public record DishPageResponse(long total, List<DishRowResponse> items) {
}
