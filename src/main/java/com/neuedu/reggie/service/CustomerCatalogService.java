package com.neuedu.reggie.service;

import com.neuedu.reggie.dto.customer.CatalogCategoryResponse;
import com.neuedu.reggie.dto.customer.CatalogDetailResponse;
import com.neuedu.reggie.dto.customer.CatalogRow;
import com.neuedu.reggie.dto.customer.OrderableProduct;
import com.neuedu.reggie.dto.dish.DishFlavorGroup;
import com.neuedu.reggie.dto.menu.MealComponent;
import com.neuedu.reggie.dto.menu.MealComponentRow;
import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.CustomerCatalogMapper;
import com.neuedu.reggie.mapper.DishFlavorMapper;
import com.neuedu.reggie.mapper.SetmealDishMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerCatalogService {

    private static final int DISH = 1;

    private final CustomerCatalogMapper catalog;
    private final DishFlavorMapper flavors;
    private final SetmealDishMapper setmealDishes;

    public CustomerCatalogService(CustomerCatalogMapper catalog, DishFlavorMapper flavors, SetmealDishMapper setmealDishes) {
        this.catalog = catalog;
        this.flavors = flavors;
        this.setmealDishes = setmealDishes;
    }

    public List<CatalogCategoryResponse> list() {
        return catalog.findAvailable().stream()
                .collect(Collectors.groupingBy(CatalogRow::categoryId, LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(rows -> {
                    CatalogRow first = rows.get(0);
                    return new CatalogCategoryResponse(first.categoryId(), first.categoryName(), first.type(),
                            rows.stream().map(CatalogRow::item).toList());
                }).toList();
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CatalogDetailResponse detail(int type, long id) {
        var item = catalog.findAvailableItem(type, id).orElseThrow(CustomerCatalogService::unavailable).item();
        if (type == DISH) {
            return new CatalogDetailResponse(item, DishFlavorGroup.fromRows(flavors.findByDish(id)), List.of());
        }
        var contents = mealContents(List.of(id)).getOrDefault(id, List.of());
        return new CatalogDetailResponse(item, List.of(), contents);
    }

    public Map<Long, List<MealComponent>> mealContents(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return setmealDishes.findMenuContents(ids).stream().collect(Collectors.groupingBy(
                MealComponentRow::setmealId, Collectors.mapping(MealComponentRow::component, Collectors.toList())));
    }

    public MenuImage image(int type, long id) {
        return (type == DISH ? catalog.findDishImage(id) : catalog.findSetmealImage(id))
                .orElseThrow(CustomerCatalogService::unavailable);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OrderableProduct lockOrderable(int type, long id) {
        return (type == DISH ? catalog.lockDish(id) : catalog.lockSetmeal(id))
                .orElseThrow(() -> new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                        "购物车中有商品已停售或删除，请刷新后调整。"));
    }

    private static BusinessException unavailable() {
        return new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "这份商品暂不可查看。");
    }
}
