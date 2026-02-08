package com.workflow.common.response;

import java.time.Instant;

public record ApiError(
        String code,     // "AUTH_INVALID", "TOKEN_EXPIRED" 같은 내부 코드
        String message,  // 사용자/프론트에 보여줄 메시지
        Instant timestamp
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }
}

