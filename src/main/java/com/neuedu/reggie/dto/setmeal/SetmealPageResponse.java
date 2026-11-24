package com.neuedu.reggie.dto.setmeal;

import java.util.List;

public record SetmealPageResponse(long total, List<SetmealRowResponse> items) {
}
