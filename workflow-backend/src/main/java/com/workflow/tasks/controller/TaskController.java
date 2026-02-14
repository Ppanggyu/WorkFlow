package com.workflow.tasks.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.dto.TempImageDTO;
import com.workflow.tasks.service.TaskService;
import com.workflow.tasks.view.TasksView;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TaskController {

	private final TaskService taskService;

	@RequestMapping(value = "/tasks", method = { RequestMethod.GET, RequestMethod.POST })
	public ResponseEntity<?> tasks(@RequestParam(value = "filter", required = false) String filter,
			@AuthenticationPrincipal User user) {

		System.out.println("filter : " + filter);

		Long userId = Long.parseLong(user.getUsername());

		List<TasksView> list = taskService.tasks(userId, filter);

		return ResponseEntity.ok(List.of(list));
	}

	@PostMapping("/taskForm")
	public ResponseEntity<?> taskForm(@RequestBody TaskCreateRequestDTO taskCreateRequestDTO,
			@AuthenticationPrincipal User user) {

		Long userId = Long.parseLong(user.getUsername());

		taskService.taskForm(taskCreateRequestDTO, userId);

		return ResponseEntity.ok(null);
	}

	@PostMapping("/imageUpload")
	public ResponseEntity<?> imageUpload(@RequestParam("file") MultipartFile file, @RequestParam("uuid") String uuid, HttpServletRequest req) {

		String imageURL = taskService.imageUpload(file, uuid, req);
		
		return ResponseEntity.ok(Map.of("imageURL", imageURL));
	}
	
	@PostMapping("/deleteImage")
	public void deleteImage(@RequestBody Map<String, String> reqPath) {

		String path = reqPath.get("path");
		taskService.deleteImage(path);

	}

}
