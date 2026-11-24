package com.neuedu.reggie.service;

import com.neuedu.reggie.dto.cart.FlavorSelection;
import com.neuedu.reggie.dto.dish.DishFlavorGroup;
import com.neuedu.reggie.entity.DishFlavorRow;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.DishFlavorMapper;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
public class MenuFlavorService {

    private static final TypeReference<List<FlavorSelection>> FLAVOR_LIST = new TypeReference<>() {};

    private final DishFlavorMapper flavors;
    private final JsonMapper json;

    public MenuFlavorService(DishFlavorMapper flavors, JsonMapper json) {
        this.flavors = flavors;
        this.json = json;
    }

    public List<FlavorSelection> validate(int type, long itemId, List<FlavorSelection> request) {
        List<FlavorSelection> selected = request == null ? List.of() : request;
        List<DishFlavorGroup> groups = type == 2 ? List.of() : DishFlavorGroup.fromRows(flavors.findByDish(itemId));
        if (!matches(groups, selected)) {
            throw new BusinessException(BusinessException.Reason.INVALID_INPUT, "请为每个口味分组选一个可用的选项。");
        }
        return selected.stream().sorted(Comparator.comparing(FlavorSelection::name)).toList();
    }

    public Map<Long, List<DishFlavorGroup>> findGroups(List<Long> dishIds) {
        if (dishIds.isEmpty()) {
            return Map.of();
        }
        return flavors.findByDishes(dishIds).stream()
                .collect(Collectors.groupingBy(DishFlavorRow::dishId))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> DishFlavorGroup.fromRows(entry.getValue().stream()
                        .map(DishFlavorRow::flavor).toList())));
    }

    public boolean matches(List<DishFlavorGroup> groups, List<FlavorSelection> selected) {
        if (groups.size() != selected.size()) {
            return false;
        }
        Map<String, String> byGroup = new HashMap<>();
        for (FlavorSelection selection : selected) {
            if (byGroup.putIfAbsent(selection.name(), selection.option()) != null) {
                return false;
            }
        }
        return groups.stream().allMatch(group -> {
            String option = byGroup.get(group.name());
            return option != null && group.options().contains(option);
        });
    }

    public String encode(List<FlavorSelection> selections) {
        return json.writeValueAsString(selections);
    }

    public List<FlavorSelection> decode(String stored) {
        return json.readValue(stored, FLAVOR_LIST);
    }
}
