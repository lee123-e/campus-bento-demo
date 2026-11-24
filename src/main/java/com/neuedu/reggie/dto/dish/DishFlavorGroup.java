package com.neuedu.reggie.dto.dish;

import com.neuedu.reggie.entity.DishFlavor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public record DishFlavorGroup(
        @NotBlank @Size(max = 40) String name,
        @NotEmpty @Size(max = 15) List<@NotBlank @Size(max = 40) String> options) {

    public static List<DishFlavorGroup> fromRows(List<DishFlavor> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(DishFlavor::groupPosition, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(group -> new DishFlavorGroup(group.get(0).name(), group.stream().map(DishFlavor::option).toList()))
                .toList();
    }
}
