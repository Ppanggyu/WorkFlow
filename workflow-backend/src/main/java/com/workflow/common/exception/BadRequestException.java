package com.workflow.common.exception;

import org.springframework.http.HttpStatus;

// 400: 클라이언트 잘못
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}