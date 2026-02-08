package com.workflow.common.exception;

import com.workflow.common.response.ApiError;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 만든 ApiException -> status/code/message 그대로 내려줌
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e) {
    	
        return ResponseEntity
                .status(e.getStatus())
                .body(ApiError.of(e.getCode(), e.getMessage()));
    }

    // JWT 라이브러리 예외 -> 401로 통일 (더 세분화 가능)
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwt(JwtException e) {
    	
        return ResponseEntity
                .status(401)
                .body(ApiError.of("TOKEN_INVALID", "토큰이 유효하지 않습니다."));
    }

    // 나머지 예상 못한 예외 -> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleEtc(Exception e) {
    	
        // 운영에선 e.printStackTrace 대신 로그로 남기는 게 정석
        return ResponseEntity
                .status(500)
                .body(ApiError.of("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}
