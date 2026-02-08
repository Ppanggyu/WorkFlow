package com.workflow.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.auth.dto.Tokens;
import com.workflow.auth.service.AuthService;
import com.workflow.common.util.CookieUtil;
import com.workflow.user.entity.UserEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserEntity userEntity, HttpServletResponse res){

        Tokens tokens = authService.login(userEntity.getEmail(), userEntity.getPassword());

        // 확인용
        // System.out.println("Access 토큰: " + tokens.getAccessToken());
        // System.out.println("Refresh 토큰: " + tokens.getRefreshToken());

        // refreshToken 쿠키 저장 (HttpOnly)
        CookieUtil.addHttpOnlyCookie(res, "refreshToken", tokens.refreshToken(), 10080);

        return ResponseEntity.ok(Map.of("accessToken", tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest req, HttpServletResponse res){

        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 자체가 없으면 -> 비로그인 204처리
        if (refreshToken == null) {
            return ResponseEntity.noContent().build();
        }

        // 유효한 경우 액세스 토큰 발급
        try {
            String newAccessToken = authService.refresh(refreshToken); // 서비스에서 다시 발급 코드
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            // 만료/위조/없는 토큰 -> 비로그인
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse res, HttpServletRequest req){

        // 먼저 요청에 들어온 refreshToken 읽기
        String refreshToken = CookieUtil.readCookie(req, "refreshToken");

        // 쿠키 삭제
        CookieUtil.deleteCookie(res, "refreshToken");

        // DB에 리프래쉬 토큰 논리적 제거하기
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // System.out.println("토큰" + req.getCookies()); // 이건 굳이 안 찍는 게 좋아(로그에 남음)
        return ResponseEntity.ok().build();
    }
}
