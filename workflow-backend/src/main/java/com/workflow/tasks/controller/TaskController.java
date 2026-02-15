package com.workflow.tasks.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.dto.TaskPageAndFilter;
import com.workflow.tasks.dto.TasksResponse;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.service.TaskService;
import com.workflow.tasks.view.TasksView;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class TaskController {

	private final TaskService taskService;

	@PostMapping("/tasks")
	public ResponseEntity<TasksResponse> tasks(@AuthenticationPrincipal User user, @RequestBody TaskPageAndFilter paging) {

		System.out.println("@RequestBody TaskPage paging.page : " + paging.page());
		System.out.println("@RequestBody TaskPage paging.size : " + paging.size());
		System.out.println("filter : " + paging.filter());
		System.out.println("status : " + paging.status());
		
		Pageable pageable = PageRequest.of(paging.page(), paging.size(), Sort.by("updatedAt").descending());

		Long userId = Long.parseLong(user.getUsername());
		
		Status selecteStatus = paging.status();

		Page<TasksView> list = taskService.tasks(userId, paging.filter(), pageable, selecteStatus);
		List<String> status = Arrays.stream(Status.values())
				.map(Enum::name).toList();

		return ResponseEntity.ok(new TasksResponse(list, status));
	}

	@PostMapping("/taskForm")
	public void taskForm(@RequestBody TaskCreateRequestDTO taskCreateRequestDTO,
			@AuthenticationPrincipal User user) {

		Long userId = Long.parseLong(user.getUsername());

		taskService.taskForm(taskCreateRequestDTO, userId);

	}
	
	@GetMapping("/taskSelected")
	public ResponseEntity<?> taskSelected(@RequestParam("taskId") Long taskId) {
		
		System.out.println("@RequestParam int taskId : " + taskId);
		
//		Long taskId1 = Long.parseLong((String) taskId.get("taskId"));
		
		TasksView selected = taskService.taskSelected(taskId);
		
		return ResponseEntity.ok(selected);
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
