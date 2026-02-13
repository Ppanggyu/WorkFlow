package com.workflow.department.controller;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
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
	
	@PostMapping("/allDepartment")
	public ResponseEntity<?> allDepartment(@AuthenticationPrincipal User user){
		
		Collection<GrantedAuthority> role = user.getAuthorities();
		Map<String, List<?>> allList = departmentService.allDepartment(role);
		
		return ResponseEntity.ok(allList);
	}

}
