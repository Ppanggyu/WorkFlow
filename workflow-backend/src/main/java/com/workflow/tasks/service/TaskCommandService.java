package com.workflow.tasks.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.service.AttachmentService;
import com.workflow.audit.dto.AuditChanges;
import com.workflow.audit.entity.AuditLogEntity;
import com.workflow.audit.repository.AuditLogRepository;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.common.file.FileStorageService;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskPriority;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.tasks.enums.TaskVisibility;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskCommandService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AttachmentService attachmentService;
    private final FileStorageService fileStorageService;

    // 업무 작성
    public TaskResponse create(TaskCreateRequest req, Long loginUserId) {
    	
    	List<AttachmentResponse> attachments = new ArrayList<>();
    	
        if (loginUserId == null) throw new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.");
        // 로그인 사용자 체크

        String title = req.titleTrimmed();
        // 제목 공백 제거

        if (title == null || title.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "제목은 필수입니다.");
        }
        // 제목 필수 체크

        UserEntity creator = userRepository.findById(loginUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
        // 작성자 정보 조회

        UserEntity assignee = (req.assigneeId() == null) ? null
                : userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "담당자를 찾을 수 없습니다."));
        // 담당자 존재 여부 확인
        
        TaskStatus status = req.statusOrDefault(); //추가
        TaskVisibility visibility = req.visibilityOrDefault();
        TaskPriority priority = req.priorityOrDefault();
        // 가시성/우선순위 기본값 처리

        if (visibility == TaskVisibility.PUBLIC) {
            Role role = creator.getRole();
            if (role == null || (role != Role.MANAGER && role != Role.ADMIN)) {
                throw new ApiException(ErrorCode.UNAUTHORIZED, "PUBLIC 업무는 매니저/관리자만 생성할 수 있습니다.");
            }
        }
        // PUBLIC 업무 생성 권한 체크

        DepartmentEntity ownerDept = creator.getDepartment();
        DepartmentEntity workDept = (assignee != null) ? assignee.getDepartment() : ownerDept;
        
        // 소유 부서 vs 업무 부서 결정
        TaskEntity task;
        
        	// 저장해서 아이디 생성
        	task = TaskEntity.builder()
        			.title(title)
        			.description(req.description())   // 초기 템프 내용
        			.status(status) //추가
        			.priority(priority)
        			.visibility(visibility)
        			.dueDate(req.dueDate())
        			.createdBy(creator)
        			.assignee(assignee)
        			.ownerDepartment(ownerDept)
        			.workDepartment(workDept)
        			.build();
        	taskRepository.save(task); // DB에 저장 후 ID 확보

        // 아이디 기반으로 tmp → final/{taskId} 이동 + 본문 URL 치환
        String descriptionFinal = fileStorageService.commitEditorImagesInContent(task.getDescription(), "tasks", task.getId());
        task.setDescription(descriptionFinal);

        taskRepository.save(task);
        // 수정된 description 재저장
        
        return TaskResponse.from(task, attachments);
        // DTO로 변환 후 반환
    }
    
//    @Tran
    public TaskResponse update(TaskCreateRequest req, Long loginUserId) {
    	List<AttachmentResponse> attachments = new ArrayList<>();

    	TaskEntity task = taskRepository.findById(req.taskId())
        		.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "게시글이 존재하지 않습니다."));
    	
    	UserEntity creator = userRepository.findById(loginUserId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "사용자가 존재하지 않습니다."));
    	
    	UserEntity assignee = (req.assigneeId() == null) ? null
                : userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "담당자를 찾을 수 없습니다."));
    	
    	if(!Objects.equals(task.getVersion(), req.version())) {
    		throw new ApiException(ErrorCode.BAD_REQUEST, "다른 사용자가 수정하였습니다. 뒤로가서 새로 시작해주세요.");
    	}
        
    	String title = req.titleTrimmed();
    	TaskStatus status = req.statusOrDefault(); //추가
        TaskVisibility visibility = req.visibilityOrDefault();
        TaskPriority priority = req.priorityOrDefault();
    	
        // 바뀐 부분 체크하는 DTO
        List<AuditChanges> changesTask = AuditChanges.from(req, task, creator);
        	
        // 프론트에서도 막지만 혹시 모를 상황 대비
        if(req.reason() == null) {
        	throw new ApiException(ErrorCode.UNAUTHORIZED, "사유를 적어주세요.");
        }
	
	    LocalDateTime now = LocalDateTime.now();
	        	
	    List<AuditLogEntity> auditLogEntity = new ArrayList<>();
	        
	    // 수정사유 외 변경사항 있을 경우
        if(changesTask.size() == 0) {
        	// reason(수정사유)만 있을 경우
        	if(!req.reason().isEmpty()) {	        	
	        	AuditLogEntity auditLogEntity1 = AuditLogEntity.builder()
	        			.task(task) // id
	        			.actor(creator) // 수정자
	        			.actionType("x") // 동작 종류
	        			.fieldName("x") // 수정된 컬럼
	        			.beforeValue("x") // 변경 전 값
	        			.afterValue("x") // 변경 후 값
	        			.reason(req.reason()) // 사유(필수)
	        			.createdAt(now)
	        			.updateGroupId(req.groupUuid())
	        			.build();
	        	auditLogEntity.add(auditLogEntity1);
	        }
        }
	        	
	    for(AuditChanges aud : changesTask) {
	    	AuditLogEntity auditLogEntity1 = AuditLogEntity.builder()
	        		.task(task) // id
	        		.actor(creator) // 수정자
	        		.actionType(aud.taskAT().toString()) // 동작 종류
	        		.fieldName(aud.fieldName()) // 수정된 컬럼
	        		.beforeValue(aud.beforeValue()) // 변경 전 값
	        		.afterValue(aud.afterValue()) // 변경 후 값
	        		.reason(req.reason()) // 사유(필수)
	        		.createdAt(now)
	        		.updateGroupId(req.groupUuid())
	        		.build();
	        auditLogEntity.add(auditLogEntity1);
	    }
	    auditLogRepository.saveAll(auditLogEntity);
	        
	        
	    attachmentService.listByTask(task.getId());
	    task.setTitle(title);
	    task.setDescription(req.description());
	    task.setStatus(status);
	    task.setPriority(priority);
	    task.setVisibility(visibility);
	    task.setDueDate(req.dueDate());
	    task.setAssignee(assignee);
	    task.setUpdatedAt(now);
	    
	 // 아이디 기반으로 tmp → final/{taskId} 이동 + 본문 URL 치환
        String descriptionFinal = fileStorageService.commitEditorImagesInContent(task.getDescription(), "tasks", task.getId());
        task.setDescription(descriptionFinal);

        
        return TaskResponse.from(task, attachments);
    }

}
