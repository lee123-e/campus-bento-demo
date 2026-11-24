package com.neuedu.reggie.dto.setmeal;

import java.util.List;

public record SetmealDetailResponse(SetmealRowResponse setmeal, String description, List<SetmealDishResponse> items) {
}
