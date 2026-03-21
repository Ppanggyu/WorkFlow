package com.workflow.audit.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.audit.dto.AuditLogResponse;
import com.workflow.audit.service.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auditLog")
public class AuditLogController {
	
	private final AuditLogService auditLogService;
	
	// 업무 수정 Log 출력
	@GetMapping("{id}")
	public List<AuditLogResponse> auditLog(@PathVariable("id") Long id,
	        @AuthenticationPrincipal UserDetails user) {
		
		Long userId = Long.parseLong(user.getUsername());
		
		return auditLogService.auditLog(id, userId);	
	}

}
