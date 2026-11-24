package com.neuedu.reggie.entity;

public record DishFlavorRow(long dishId, int groupPosition, int optionPosition, String name, String option) {

    public DishFlavor flavor() {
        return new DishFlavor(groupPosition, optionPosition, name, option);
    }
}
