package com.workflow.attachments.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.attachments.service.AttachmentsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttachmentsController {
	
	private final AttachmentsService attachmentsService;
	
	@GetMapping("/{taskId}/file/{attachmentsId}")
	public ResponseEntity<?> dwonloadAtt(@AuthenticationPrincipal User user, 
			@PathVariable("taskId") Long taskId, @PathVariable("attachmentsId") Long attachmentsId) throws Exception {
		
		Long userId = Long.parseLong(user.getUsername());
		
		return attachmentsService.dwonloadAtt(userId, taskId, attachmentsId);
	}

}
