package com.workflow.user.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.user.dto.UserSimpleResponse;
import com.workflow.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
	
	private final UserService userService;  // User 관련 DB 접근

	// 업무 담당자 선택용 리스트 조회
	@GetMapping("/assigneelist")
	public List<UserSimpleResponse> list(@AuthenticationPrincipal UserDetails principal) {
		
		Long userId = Long.parseLong(principal.getUsername());
	    // DB에서 모든 사용자와 소속 부서 정보 조회
	    return userService.list(userId);
	}
}
