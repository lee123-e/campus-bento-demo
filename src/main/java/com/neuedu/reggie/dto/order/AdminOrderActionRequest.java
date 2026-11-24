package com.neuedu.reggie.dto.order;

import jakarta.validation.constraints.NotNull;

public record AdminOrderActionRequest(@NotNull Action action) {

    public enum Action {
        DISPATCH,
        COMPLETE
    }
}
