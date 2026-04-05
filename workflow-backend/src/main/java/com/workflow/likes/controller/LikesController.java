package com.workflow.likes.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.likes.service.LikesService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/likes")
public class LikesController {
	
	private final LikesService likesService;
	
	// 즐겨찾기
	@PostMapping("/{taskId}")
	public void onLikes(@PathVariable("taskId") Long taskId,
	        @AuthenticationPrincipal UserDetails principal) {
		Long userId = Long.parseLong(principal.getUsername());
		likesService.onLike(taskId, userId);
	}
	
	// 즐겨찾기 취소
	@DeleteMapping("/{taskId}")
	public void delLikes(@PathVariable("taskId") Long taskId,
			@AuthenticationPrincipal UserDetails principal) {
		Long userId = Long.parseLong(principal.getUsername());
		likesService.delLike(taskId, userId);
	}

}
