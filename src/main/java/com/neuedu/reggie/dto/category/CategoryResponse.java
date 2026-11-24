package com.neuedu.reggie.dto.category;

import com.neuedu.reggie.entity.Category;
import java.time.LocalDateTime;

public record CategoryResponse(long id, int type, String name, int sort, LocalDateTime updatedAt) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getCategoryType(), category.getName(),
                category.getSortOrder(), category.getUpdatedAt());
    }
}
