package com.workflow.tasks.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.service.TaskService;
import com.workflow.tasks.view.TasksView;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TaskController {
	
	private final TaskService taskService;
	
	@RequestMapping(value = "/tasks", method = {RequestMethod.GET, RequestMethod.POST})
	public ResponseEntity<?> tasks(@RequestParam(value = "filter", required = false) String filter, @AuthenticationPrincipal User user){
		
		System.out.println("filter : " + filter);
		
		Long userId = Long.parseLong(user.getUsername());
		
		List<TasksView> list = taskService.tasks(userId, filter);
		
		return ResponseEntity.ok(List.of(list));
	}
	
	@PostMapping("/taskForm")
	public ResponseEntity<?> taskForm(@RequestBody TaskCreateRequestDTO taskCreateRequestDTO, @AuthenticationPrincipal User user) {
		
		Long userId = Long.parseLong(user.getUsername());
		
		taskService.taskForm(taskCreateRequestDTO, userId);
		
		return ResponseEntity.ok(null);
	}

}
