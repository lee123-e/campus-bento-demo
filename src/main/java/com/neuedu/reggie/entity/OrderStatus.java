package com.neuedu.reggie.entity;

public enum OrderStatus {

    PENDING_PAYMENT(1), AWAITING_DISPATCH(2), DISPATCHED(3), COMPLETED(4), CANCELLED(5);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
