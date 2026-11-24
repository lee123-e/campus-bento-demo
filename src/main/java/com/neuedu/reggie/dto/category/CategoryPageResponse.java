package com.neuedu.reggie.dto.category;

import java.util.List;

public record CategoryPageResponse(long total, List<CategoryResponse> items) {
}
