package com.neuedu.reggie.service;

import com.neuedu.reggie.entity.Dish;
import com.neuedu.reggie.exception.BusinessException;
import com.neuedu.reggie.mapper.DishMapper;
import com.neuedu.reggie.mapper.SetmealDishMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class MenuAvailabilityService {

    private final DishMapper dishes;
    private final SetmealDishMapper setmealDishes;

    public MenuAvailabilityService(DishMapper dishes, SetmealDishMapper setmealDishes) {
        this.dishes = dishes;
        this.setmealDishes = setmealDishes;
    }

    public void requireDishes(List<Long> ids, boolean requireOnSale) {
        if (ids.isEmpty()) {
            throw new BusinessException(BusinessException.Reason.RULE_CONFLICT, "请先为套餐添加菜品。");
        }
        List<Dish> selected = dishes.lockForMenuUpdate(ids);
        if (selected.size() != ids.size()) {
            throw new BusinessException(BusinessException.Reason.RECORD_NOT_FOUND, "所选菜品已变动，请刷新后重试。");
        }
        if (requireOnSale) {
            selected.stream().filter(dish -> !dish.getOnSale()).findFirst().ifPresent(dish -> {
                throw new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                        "「" + dish.getName() + "」已停售。请先启售该菜品或将它移出套餐。");
            });
        }
    }

    public void requireCanStopDishes(List<Long> ids) {
        requireDishes(ids, false);
        setmealDishes.findActiveSetmealName(ids).ifPresent(name -> {
            throw new BusinessException(BusinessException.Reason.RULE_CONFLICT,
                    "所选菜品仍在启售的「" + name + "」套餐中，请先停售该套餐。");
        });
    }
}
