package com.workflow.tasks.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.workflow.common.dto.PageResponse;
import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.service.TaskCommandService;
import com.workflow.tasks.service.TaskQueryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tasks") // 공통 URL prefix
public class TaskController {
	
	private final TaskCommandService taskCommandService;
	private final TaskQueryService taskQueryService;	
	
	// 업무 목록 조회
	@GetMapping
	public ResponseEntity<PageResponse<TaskResponse>> list(
	    @RequestParam(name = "scope", required = false, defaultValue = "all") String scope, // 조회 범위(all, my, dept 등)
	    @RequestParam(name = "status", required = false) TaskStatus status, // 필터: 업무 상태
	    @RequestParam(name = "page", required = false, defaultValue = "0") int page, // 페이지 번호(0부터 시작)
	    @RequestParam(name = "size", required = false, defaultValue = "9") int size, // 한 페이지에 보여줄 개수
	    @AuthenticationPrincipal UserDetails principal // Spring Security 인증 정보
	) {
		Long userId = Long.parseLong(principal.getUsername()); // username에 실제 userId 저장
		Page<TaskResponse> result = taskQueryService.list(scope, status, userId, page, size); // 서비스 호출
		
		return ResponseEntity.ok(PageResponse.from(result)); // 페이징 응답 포맷 변환 후 반환
	}
	
	// 업무 생성
	@PostMapping("/create")
	public ResponseEntity<TaskResponse> create(
	        @Valid @RequestBody TaskCreateRequest req, // 요청 DTO, 유효성 검증
	        @AuthenticationPrincipal UserDetails principal // 로그인 사용자 정보
	) {
	    if (principal == null) return ResponseEntity.status(401).build(); // 인증 안 됐으면 401
	    Long userId = Long.parseLong(principal.getUsername()); // username에 id 들어있음
	    return ResponseEntity.ok(taskCommandService.create(req, userId)); // 생성 후 DTO 반환
	}
	
	// 업무 생성
	@PostMapping("/update")
	public ResponseEntity<TaskResponse> update(
			@Valid @RequestBody TaskCreateRequest req, // 요청 DTO, 유효성 검증
			@AuthenticationPrincipal UserDetails principal // 로그인 사용자 정보
			) {
		if (principal == null) return ResponseEntity.status(401).build(); // 인증 안 됐으면 401
		Long userId = Long.parseLong(principal.getUsername()); // username에 id 들어있음
		return ResponseEntity.ok(taskCommandService.update(req, userId)); // 생성 후 DTO 반환
	}
	
	// 업무 상세 조회
	@GetMapping("/{id}")
	public TaskResponse detail(
	        @PathVariable("id") Long id, // 조회할 업무 ID
	        @AuthenticationPrincipal UserDetails principal, // 로그인 사용자 정보
	        HttpServletRequest req
	) {
		
		Long userId = Long.parseLong(principal.getUsername()); // 사용자 ID
		String uri = req.getRequestURI();
		boolean isEdit = uri.endsWith("/edit");
	    return taskQueryService.detail(id, userId, isEdit); // 서비스 호출 후 DTO 반환
	}
	
	// 업무 수정 페이지 데이터 출력
	@GetMapping("/{id}/edit")
	public TaskResponse update(
			@PathVariable("id") Long id, // 조회할 업무 ID
			@AuthenticationPrincipal UserDetails principal, // 로그인 사용자 정보
			HttpServletRequest req
			) {
		
		String uri = req.getRequestURI();
		boolean isEdit = uri.endsWith("/edit");
		Long userId = Long.parseLong(principal.getUsername()); // 사용자 ID
		return taskQueryService.detail(id, userId, isEdit); // 서비스 호출 후 DTO 반환
	}
	
	// 업무 논리 삭제
	@DeleteMapping("/{id}")
	public void delete(@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails principal,
			@RequestBody String reason) {
		
		Long userId = Long.parseLong(principal.getUsername());
		taskQueryService.delete(id, userId, reason);
		
	}

}
