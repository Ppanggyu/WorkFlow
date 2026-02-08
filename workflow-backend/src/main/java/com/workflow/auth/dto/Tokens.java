package com.workflow.auth.dto;
// record: 데이터만 담는 불변 객체 DTO를 아주 간단하게 만드는 문법
// private final 필드
// 전체 생성자
// getter (accessToken(), refreshToken())
// equals()
// hashCode()
// toString()
// 를 자동으로 만들어 줌.
// 단, 생성 후 값 변경 불가
// 토큰, 응답 등 값이 변경 안되는 객체에 최고로 안전.
// Jackson / Spring 기본 지원
public record Tokens(String accessToken, String refreshToken) {}

