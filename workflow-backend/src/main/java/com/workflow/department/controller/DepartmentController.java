package com.workflow.department.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.department.service.DepartmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DepartmentController {
	
	private final DepartmentService departmentService;
	
	// task 작성 페이지 우선순위, 공개범위, 담당자
	@PostMapping("/allDepartment")
	public ResponseEntity<?> allDepartment(@AuthenticationPrincipal User user){
		
		Map<String, List<?>> allList = departmentService.allDepartment(user);
		
		return ResponseEntity.ok(allList);
	}

}
