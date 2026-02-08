package com.workflow.common.exception;

import org.springframework.http.HttpStatus;

// 401: 인증 안됨
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}