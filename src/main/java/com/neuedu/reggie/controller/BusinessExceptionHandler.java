package com.neuedu.reggie.controller;

import com.neuedu.reggie.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BusinessExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handle(BusinessException exception) {
        HttpStatus status = switch (exception.reason()) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case RECORD_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case RULE_CONFLICT -> HttpStatus.CONFLICT;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case SESSION_EXPIRED -> HttpStatus.UNAUTHORIZED;
        };
        return ProblemDetail.forStatusAndDetail(status, exception.getMessage());
    }
}
