package com.workflow.common.exception;

import org.springframework.http.HttpStatus;

// 401: 인증 안됨
public class UnauthorizedException extends ApiException {
	
	private static final long serialVersionUID = 1L;
	
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}