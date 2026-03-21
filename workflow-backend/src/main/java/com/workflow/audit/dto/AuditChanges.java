package com.workflow.audit.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.workflow.attachment.entity.AttachmentEntity;
import com.workflow.audit.enums.AuditAttachmentActionType;
import com.workflow.audit.enums.AuditTaskActionType;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.tasks.dto.TaskCreateRequest;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;

import io.jsonwebtoken.lang.Arrays;

public record AuditChanges(
		String fieldName,
		String beforeValue,
		String afterValue,
		AuditTaskActionType taskAT,
		AuditAttachmentActionType attachmentAT
		) {

	public static List<AuditChanges> from (TaskCreateRequest req, TaskEntity task, UserEntity user){
		List<AuditChanges> change = new ArrayList<>();
		
		AuditTaskActionType updates = null;
		
		if(taskUpdateEvent(req, task)) {
			updates = AuditTaskActionType.TASK_UPDATE;
		}
		
		if(!Objects.equals(task.getTitle(), req.title())) {
			change.add(new AuditChanges(
					"title",
					task.getTitle(),
					req.title(), updates, null));
		}
		if(!Objects.equals(task.getDescription(), req.description())) {
			change.add(new AuditChanges(
					"description",
					task.getDescription(),
					req.description(), updates, null));
		}
		if(!Objects.equals(task.getPriority(), req.priority())) {
			change.add(new AuditChanges(
					"priority",
					task.getPriority().toString(),
					req.priority().toString(), updates, null));
		}
		if(!Objects.equals(task.getVisibility(), req.visibility())) {
			change.add(new AuditChanges(
					"visibility",
					task.getVisibility().toString(),
					req.visibility().toString(), updates, null));
		}
		if(!Objects.equals(task.getDueDate(), req.dueDate())) {
			change.add(new AuditChanges(
					"dueDate",
					task.getDueDate().toString(),
					req.dueDate().toString(), updates, null));
		}
		
		Long currentAssignee = task.getAssignee() != null ?
				task.getAssignee().getId() : null;
		
		if(!Objects.equals(currentAssignee, req.assigneeId())) {
			change.add(new AuditChanges(
					"assignee",
					currentAssignee != null ? currentAssignee.toString() : null
					,req.assigneeId() != null ? req.assigneeId().toString() : null
					, updates, null));
		}
		
		if(!Objects.equals(req.status(), req.beforeStatus())) {
			List<String> updateStatus = statusUpdateCheck(req, task, user);
			for(int i = 0; i < updateStatus.size() - 1; i++ ) {
				change.add(new AuditChanges(
						"status",
						updateStatus.get(i),
						updateStatus.get(i + 1),
						updates, null
						));
			}
			if (updateStatus.size() == 1) {
				change.add(new AuditChanges(
						"status",
						req.beforeStatus().name(),
						updateStatus.get(0),
						updates,
						null
						));
			}
		}
		
		return change;
	}
	
	public static AuditChanges from (List<AttachmentEntity> dbList, List<AttachmentEntity> entityList){
		
		Set<String> db = dbList.stream()
				.map(AttachmentEntity::getStoredFilename)
				.collect(Collectors.toSet());
		
		Set<String> en = entityList.stream()
				.map(AttachmentEntity::getStoredFilename)
				.collect(Collectors.toSet());
		
		// 추가된 attachment의 StoredFilename
		Set<String> added = new HashSet<>(en);
		added.removeAll(db);
		
		// 삭제된 attachment의 StoredFilename
		Set<String> deleted = new HashSet<>(db);
		deleted.removeAll(en);
		
		if(added.isEmpty() && deleted.isEmpty()) {
			return null;
		}
		
		String beforeValue = deleted.isEmpty() ? null : "삭제 : " + deleted;
		String afterValue = added.isEmpty() ? null : "추가 : " + added;
		
		AuditAttachmentActionType actionType;

		if (!added.isEmpty() && !deleted.isEmpty()) {
		    actionType = AuditAttachmentActionType.ATTACHMENT_CHANGE;
		} else if (!added.isEmpty()) {
		    actionType = AuditAttachmentActionType.ATTACHMENT_ADD;
		} else {
		    actionType = AuditAttachmentActionType.ATTACHMENT_DELETE;
		}

	    // 추가 + 삭제 동시에 발생
	    return new AuditChanges(
	            "Attachment",
	            beforeValue,
	            afterValue,
	            null,
	            actionType
	    );
	}
	
	private static List<String> statusUpdateCheck(TaskCreateRequest req, TaskEntity task, UserEntity user) {
		
		boolean isAdmin = user.getRole() == Role.ADMIN; // ADMIN
        boolean isManager = user.getRole() == Role.MANAGER; // MANAGER
        boolean isUser = user.getRole() == Role.USER; // USER
        boolean isDepartment = task.getWorkDepartment().getId() == user.getDepartment().getId(); // 부서
        boolean isCreatorUser = task.getCreatedBy().getId() == user.getId(); // 작성자
        boolean isAssignee =  task.getAssignee() != null ? // 담당자
        		task.getAssignee().getId() == user.getId()
        		: false;
        
        // ADMIN or (MANGER and 담당부서)
        if(isAdmin || (isManager && isDepartment)) {
        	// 다 가능
        }
        // USER and (업무 생성자 or 업무 담당자) 
        else if(isUser && (isCreatorUser || isAssignee)) {
        	
        	// TODO -> IN_PROGRESS or ON_HOLD or REVIEW / IN_PROGRESS -> REVIEW or ON_HOLD / REVIEW -> IN_PROGRESS or ON_HOLD
        	if((req.beforeStatus() == TaskStatus.TODO ||
        			req.beforeStatus() == TaskStatus.IN_PROGRESS ||
        			req.beforeStatus() == TaskStatus.REVIEW
        			) && (
        			req.status() == TaskStatus.IN_PROGRESS ||
        			req.status() == TaskStatus.ON_HOLD ||
        			req.status() == TaskStatus.REVIEW)) {
        		
        	}else {
        		throw new ApiException(ErrorCode.FORBIDDEN, "잘못된 접근 입니다.");
        	}
        } else {
        	throw new ApiException(ErrorCode.FORBIDDEN, "잘못된 접근 입니다.");
        }
		
		TaskStatus[] statusValue = TaskStatus.values();
		// Enum 순서대로 인덱스 반환
		int start = req.beforeStatus().ordinal();
		int end = req.status().ordinal();
		
		// 현재상태 -> ON_HOLD or CANCLED 
		if(req.status() == TaskStatus.ON_HOLD || req.status() == TaskStatus.CANCELED) {
			return List.of(req.status().name());
		}
		
		// ON_HOLD or CANCLED -> 현재상태 ( ON_HOLD or CANCLED -> IN_PROGRESS -> 현재상태 )
		if(req.beforeStatus() == TaskStatus.ON_HOLD || req.beforeStatus() == TaskStatus.CANCELED) {
			int progress = TaskStatus.IN_PROGRESS.ordinal();
			if(progress > end) {
				throw new ApiException(ErrorCode.FORBIDDEN, "잘못된 접근 입니다.");
			}
			
			List<String> result = new ArrayList<>();
			
			result.add(req.beforeStatus().name());
			
			// ON_HOLD or CANCLED 담기 위한
			result.addAll(Arrays.asList(statusValue).subList(progress, end + 1) .stream().map(Enum::name).toList());
			
			return result;
		}
		
		// 마음대로 못바꾸게 ( 보안용 )
		if(start > end) {
			throw new ApiException(ErrorCode.FORBIDDEN, "잘못된 접근 입니다.");
		}
		
		// 정상적인 현재상태 적용
		// List로 변환하고 부분 범위 추출 / start부터 end까지 -> end 인덱스는 포함 안하기때문에 +1
		return Arrays.asList(statusValue)
				.subList(start, end + 1)
				.stream()
				.map(Enum::name)
				.toList();
	}
	
	private static boolean taskUpdateEvent(TaskCreateRequest req, TaskEntity task) {
		Long currentAssignee = task.getAssignee() != null ?
				task.getAssignee().getId() : null;
		
		return
				!Objects.equals(task.getTitle(), req.title()) ||
				!Objects.equals(task.getDescription(), req.description()) ||
				!Objects.equals(task.getPriority(), req.priority()) ||
				!Objects.equals(task.getVisibility(), req.visibility()) ||
				!Objects.equals(currentAssignee, req.assigneeId()) ||
				!Objects.equals(req.status(), req.beforeStatus()) ||
				!Objects.equals(task.getDueDate(), req.dueDate());
	}
	
}
