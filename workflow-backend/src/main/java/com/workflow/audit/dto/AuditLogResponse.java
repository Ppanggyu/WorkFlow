package com.workflow.audit.dto;

import java.time.LocalDateTime;

import com.workflow.audit.entity.AuditLogEntity;
import com.workflow.user.dto.UserSimpleResponse;

public record AuditLogResponse(
		Long id,
		Long taskId,
		UserSimpleResponse actor,
		String actionType,
		String beforeValue,
		String afterValue,
		String fieldName,
		String reason,
		LocalDateTime createdAt,
		String updateGroupId
		) {
	
	public static AuditLogResponse from(AuditLogEntity auditLogEntity) {
		
		UserSimpleResponse actor = new UserSimpleResponse(
				auditLogEntity.getActor().getId(),
				auditLogEntity.getActor().getName(),
				auditLogEntity.getActor().getDepartment().getName(),
				auditLogEntity.getActor().getRole()
		);
		
		return new AuditLogResponse(
				auditLogEntity.getId(),
				auditLogEntity.getTask().getId(),
				actor,
				auditLogEntity.getActionType(),
				auditLogEntity.getBeforeValue(),
				auditLogEntity.getAfterValue(),
				auditLogEntity.getFieldName(),
				auditLogEntity.getReason(),
				auditLogEntity.getCreatedAt(),
				auditLogEntity.getUpdateGroupId()
				);
	}

}
