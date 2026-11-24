package com.neuedu.reggie.exception;

public class BusinessException extends RuntimeException {

    private final Reason reason;

    public BusinessException(Reason reason, String message) {
        this(reason, message, null);
    }

    public BusinessException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }

    public enum Reason {
        INVALID_INPUT,
        RECORD_NOT_FOUND,
        RULE_CONFLICT,
        RATE_LIMITED,
        SERVICE_UNAVAILABLE,
        SESSION_EXPIRED
    }
}
