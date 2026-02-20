package com.workflow.attachments.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachments.service.AttachmentsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttachmentsController {
	
	private final AttachmentsService attachmentsService;
	
	@PostMapping("/attachmentsSave")
	public void saveAttachments(@AuthenticationPrincipal User user, @RequestParam("file") List<MultipartFile> file) {
		
		Long userId = Long.parseLong(user.getUsername());
		
		attachmentsService.saveAttachments(userId, file);
		
	}

}
