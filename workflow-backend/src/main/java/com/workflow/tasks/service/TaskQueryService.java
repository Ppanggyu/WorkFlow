package com.workflow.tasks.service;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.service.AttachmentService;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.department.enums.Department;
import com.workflow.likes.repository.LikesRepository;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskQueryService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final LikesRepository likesRepository;

    // 업무 목록 조회
    public Page<TaskResponse> list(String scope, TaskStatus status, String dept, Long userId, int page, int size) {

        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        // 로그인 사용자 확인

        if (scope == null || scope.isBlank()) scope = "all";
        // 기본 scope 설정

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        // 페이지네이션, 최소 1~최대 100 제한, 최신순 정렬

        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();
        // 로그인 사용자 정보 + 부서 ID
        
        Page<TaskEntity> result;
        
        boolean isAdmin = me.getRole() == Role.ADMIN;
        boolean isManager = me.getRole() == Role.MANAGER;
        List<Long> team = new ArrayList<>();
        
        if(isAdmin && scope.equals("deleted")) scope = "adminWithDeleted";
        
        if(isManager && scope.equals("deleted")) { 
        	scope = "managerWithDeleted";
        	team = userRepository.findByDepartmentId(myDeptId)
        			.stream().filter(user -> user.getRole() != Role.ADMIN)
        			.map(UserEntity::getId).toList();
        }
        
        
        // 자바 14 이상부터 switch expression 사용
        switch (scope) {

            // 전체 업무: 내가 볼 수 있는 모든 업무
            case "all" -> result = (status == null && dept == null)
                    ? taskRepository.findAllVisibleForUser(userId, myDeptId, isAdmin, pageable)
                    : taskRepository.findAllVisibleForUserByStatus(userId, myDeptId, status, dept, isAdmin, pageable);

            // 전사 업무: PUBLIC만
            case "public" -> result = (status == null && dept == null)
                    ? taskRepository.findPublicOnly(pageable)
                    : taskRepository.findPublicOnlyByStatusAndDept(status, dept, pageable);

            // 우리팀 업무: 우리 팀만 + PRIVATE는 (작성자/담당자=나)만 예외 허용
            case "team" -> result = (status == null)
                    ? taskRepository.findTeamVisibleForUser(userId, myDeptId, pageable)
                    : taskRepository.findTeamVisibleForUserByStatus(userId, myDeptId, status, pageable);

            // 내가 만든 업무: 내가 작성자인 것만
            case "created" -> result = (status == null)
                    ? taskRepository.findByIsDeletedFalseAndCreatedBy_Id(userId, pageable)
                    : taskRepository.findByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status, pageable);

            // 담당 업무: 내가 담당자인 것만
            case "assigned" -> result = (status == null)
                    ? taskRepository.findByIsDeletedFalseAndAssignee_Id(userId, pageable)
                    : taskRepository.findByIsDeletedFalseAndAssignee_IdAndStatus(userId, status, pageable);
            
            // 즐겨찾기
            case "like" -> result = (status == null)
            		? taskRepository.findByLikeTask(userId, pageable)
            				: taskRepository.findLikeTaskWithStatus(userId, status, pageable);
            
            // 삭제된 업무
            case "deleted" -> result = (status == null)
            		? taskRepository.findDeletedList(userId, pageable)
            		: taskRepository.findDeletedListWithStatus(userId, status, pageable);
            
            case "adminWithDeleted" -> result = (status == null && dept == null)
            		? taskRepository.findByIsDeletedTrue(pageable)
                    : taskRepository.findIsDeletedTaskWithStatusAndDept(status, dept, pageable);
            
            case "managerWithDeleted" -> result = (status == null)
            		? taskRepository.findDeletedListWithManager(team, pageable)
            		: taskRepository.findDeletedListWithManagerAndStatus(team, status, pageable);

            default -> throw new ApiException(ErrorCode.BAD_REQUEST, "scope 값이 올바르지 않습니다.");
            // 범위 값 검증
        }
        
        boolean isDeleted = "deleted".contains(scope);
        
        // Task의 Id 추출하기 위한 준비
        // 페이징된 Task 가져오기
        List<TaskEntity> task = result.getContent();
        // 가져온 Task의 ID만 추출
        List<Long> taskIds = task.stream()
        		.map(TaskEntity::getId)
        		.toList();
        // 로그인한 사용자와 추출한 ID를 기준으로 즐겨찾기 조회
        Set<Long> likedTaskIds = new HashSet<>(
        		likesRepository.findLikedTaskIds(userId, taskIds));
        
        
        return result.map(t -> {
            long cnt = attachmentService.countActiveByTask(t.getId());
            // 즐겨찾기에서 조회한 task_id들과 조회한 task의 id를 기준으로 있으면 true, 없으면 false
            boolean liked = !isDeleted && likedTaskIds.contains(t.getId());
            return TaskResponse.from(t, cnt, 
            		liked);
        });
    }

    // 업무 상세 조회
    public TaskResponse detail(Map<String, Object> map, Long userId, boolean isEdit) {

        if (userId == null) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        
        Long taskId = (Long) map.get("id");
        String scope = (String) map.get("scope");
        
        // 로그인 체크
        UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        Long myDeptId = me.getDepartment().getId();
        // 사용자 부서 ID
        
        TaskEntity task;
        if("deleted".equals(scope)) {
        	task = taskRepository.findByIdAndIsDeletedTrue(taskId)
        			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
        	// 접근 가능한 Task 상세 조회
        }else {
        	task = taskRepository.findByIdAndIsDeletedFalse(taskId)
        			.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
        	// 접근 가능한 Task 상세 조회
        }
        
        boolean isAdmin = me.getRole() == Role.ADMIN;
        boolean isManager = me.getRole() == Role.MANAGER;
        boolean isCreator = task.getCreatedBy().getId() == me.getId();
        boolean isAssignee = task.getAssignee() != null ? (task.getAssignee().getId() == me.getId()) : false;
        boolean isDepartment = task.getWorkDepartment().getId() == me.getDepartment().getId(); // 부서
        boolean isPublic = task.getVisibility() == TaskVisibility.PUBLIC;
        
        // 타 부서가 url로 못보게
        boolean canAccess = isPublic ||
        		isAdmin ||
        		(isManager && isDepartment) ||
        		(isCreator || isAssignee || isDepartment);
        if (!canAccess) {
            throw new ApiException(ErrorCode.FORBIDDEN, "권한X");
        }
        
        // 현재 uri가 edit인지 확인
        if(isEdit) {
        	// 업무 작성자, 담당자, 로그인한 유저의 권한 필터 ( 3조건 안맞을 시 throw )
        	if(!(isCreator || isAssignee || isAdmin)) {
        		throw new ApiException(ErrorCode.FORBIDDEN);
        	}
        }

        var attachments = attachmentService.listByTask(taskId);
        // 첨부 목록 로딩
        
        // 즐겨찾기 조회용 준비
        boolean isLiked = likesRepository.existsByUserId_IdAndTaskId_Id(userId, taskId);

        return TaskResponse.from(task, attachments, isLiked);
        // TaskResponse DTO 반환, 첨부 포함
    }

    // KPI 출력
    public Map<String, Map<String, Long>> kpi(Long userId) {

        Map<String, Long> assigned = new LinkedHashMap<>();
        Map<String, Long> created = new LinkedHashMap<>();
        // LinkedHashMap: 삽입 순서 유지 (출력 순서 일관성)

        for (TaskStatus status : EnumSet.allOf(TaskStatus.class)) {
            assigned.put(status.name(),
                    taskRepository.countByIsDeletedFalseAndAssignee_IdAndStatus(userId, status));
            created.put(status.name(),
                    taskRepository.countByIsDeletedFalseAndCreatedBy_IdAndStatus(userId, status));
        }

        return Map.of(
                "assigned", assigned,
                "created", created
        );
    }
    
	public Page<TaskResponse> dashBoardTask(Long userId, int page, int size, String scope) {
		
		Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
		
		UserEntity me = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
		
		Page<TaskResponse> res;
		
        if(scope.equals("created")) {
        	res = taskRepository.findByCreatedByIdAndIsDeletedFalse(me.getId(), pageable)
        			.map(t -> {
        				boolean isLiked = likesRepository.existsByUserId_IdAndTaskId_Id(userId, t.getId());
        				long cnt = attachmentService.countActiveByTask(t.getId());
        				return TaskResponse.from(t, cnt, isLiked);});
        }else {
        	res = taskRepository.findByAssigneeIdAndIsDeletedFalse(me.getId(), pageable)
        			.map(t -> {
        				boolean isLiked = likesRepository.existsByUserId_IdAndTaskId_Id(userId, t.getId());
        				long cnt = attachmentService.countActiveByTask(t.getId());
        				return TaskResponse.from(t, cnt, isLiked);});
        }
		
		return res;
	}

}
