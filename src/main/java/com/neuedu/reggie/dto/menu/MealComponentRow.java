package com.neuedu.reggie.dto.menu;

public record MealComponentRow(long setmealId, String name, int quantity) {

    public MealComponent component() {
        return new MealComponent(name, quantity);
    }
}
