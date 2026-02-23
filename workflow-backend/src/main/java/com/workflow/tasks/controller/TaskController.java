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

import com.workflow.attachments.entity.AttachmentsEntity;
import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.dto.TaskDTO;
import com.workflow.tasks.dto.TaskFilesDTO;
import com.workflow.tasks.dto.TaskPageAndFilter;
import com.workflow.tasks.dto.TaskSelectedRes;
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
		
		Pageable pageable = PageRequest.of(paging.page(), paging.size(), Sort.by("updatedAt").descending());

		Long userId = Long.parseLong(user.getUsername());
		
		Status selecteStatus = paging.status();

		Page<TaskDTO> list = taskService.tasks(userId, paging.filter(), pageable, selecteStatus);
		
		System.out.println("paging : " + paging.page());
		System.out.println("paging : " + paging.size());
		
		List<String> status = Arrays.stream(Status.values())
				.map(Enum::name).toList();
		
	    TasksResponse response = new TasksResponse(
	            list.getContent(),        // 실제 데이터 리스트
	            status,                   // 전체 status
	            list.getTotalPages(),     // 총 페이지 수
	            list.getTotalElements()   // 전체 항목 수
	        );

		return ResponseEntity.ok(response);
	}

	@PostMapping("/taskForm")
	public void taskForm(@RequestBody TaskCreateRequestDTO taskCreateRequestDTO,
			@AuthenticationPrincipal User user) {

		Long userId = Long.parseLong(user.getUsername());

		taskService.taskForm(taskCreateRequestDTO, userId);

	}
	
	@GetMapping("/taskSelected")
	public ResponseEntity<?> taskSelected(@RequestParam("taskId") Long taskId) {
		
		TaskSelectedRes seletedRes = taskService.taskSelected(taskId);
		
		return ResponseEntity.ok(seletedRes);
	}

	@PostMapping("/imageUpload")
	public ResponseEntity<?> imageUpload(@RequestParam("file") List<MultipartFile> file, @RequestParam("uuid") String uuid, HttpServletRequest req) {

		Map<String, Object> maps = taskService.imageUpload(file, uuid, req);
		List<TaskFilesDTO> image = (List<TaskFilesDTO>) maps.get("taskFiles");
		
		return ResponseEntity.ok(image);
	}
	
	@PostMapping("/deleteImage")
	public void deleteImage(@RequestBody Map<String, String> reqPath) {

		String path = reqPath.get("path");
		taskService.deleteImage(path);

	}
	
	@PostMapping("/fileUpload")
	public ResponseEntity<?> fileUpload(@RequestParam("file") List<MultipartFile> file, @RequestParam("uuid") String uuid, HttpServletRequest req) {
		
		for(int i = 0; i < file.size(); i++) {
			System.out.println(file.get(i).getOriginalFilename());
		}
		
		Map<String, Object> maps = taskService.imageUpload(file, uuid, req);
		List<TaskFilesDTO> file1 = (List<TaskFilesDTO>) maps.get("taskFiles");
		
		return ResponseEntity.ok(file1);
	}
	
	@PostMapping("/deleteFile")
	public void deleteFile(@RequestBody Map<String, String> reqPath) {
		
		String path = reqPath.get("path");
		
		taskService.deleteImage(path);

	}

}
