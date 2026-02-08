package com.workflow.common.exception;

import org.springframework.http.HttpStatus;

// 403: 인증은 됐지만 권한 없음.
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}
