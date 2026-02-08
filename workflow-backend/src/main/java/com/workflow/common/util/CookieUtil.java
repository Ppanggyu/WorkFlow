package com.workflow.common.util;

import org.springframework.http.ResponseCookie;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

    private CookieUtil() {} // 유틸 클래스

    // 쿠키 생성
    public static void addHttpOnlyCookie(HttpServletResponse res, String name, String value, int maxMin) {
    	
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)      // JS 접근 불가
                .secure(false)       // HTTPS면 true
                .sameSite("Lax")     // CSRF 방어
                .path("/")
                .maxAge(maxMin * 60L)
                .build();

        res.addHeader("Set-Cookie", cookie.toString());
    }

    // 쿠키 삭제
    public static void deleteCookie(HttpServletResponse res, String name) {
    	
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();

        res.addHeader("Set-Cookie", cookie.toString());
    }

    // 쿠키 읽기
    public static String readCookie(HttpServletRequest req, String name) {
    	
        if (req.getCookies() == null) return null;

        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
