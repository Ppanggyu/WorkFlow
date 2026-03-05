package com.workflow.tasks.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.workflow.department.dto.DepartmentDTO;
import com.workflow.tasks.entity.TasksEntity;
import com.workflow.tasks.enums.Priority;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.user.dto.UserDTO;

public record TaskDTO(
		Long id,
        String title,
        String description,
        Status status,
        Priority priority,
        Visibility visibility,
        LocalDate dueDate,
        String holdReason,
        String cancelReason,
        Boolean isDeleted,
        UserDTO createdBy,
        UserDTO assigneeId,
        DepartmentDTO ownerDepartmentId,
        DepartmentDTO workDepartmentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
		) {
	
	public static TaskDTO toDto(TasksEntity entity) {
		return new TaskDTO(entity.getId(), entity.getTitle(), entity.getDescription(), entity.getStatus(),
				entity.getPriority(), entity.getVisibility(), entity.getDueDate(), entity.getHoldReason(),
				entity.getCancelReason(), entity.getIsDeleted(),

				UserDTO.toUserDto(entity.getCreatedBy()), UserDTO.toUserDto(entity.getAssigneeId()),
				DepartmentDTO.toDepartmentDto(entity.getOwnerDepartmentId()), DepartmentDTO.toDepartmentDto(entity.getWorkDepartmentId()),

				entity.getCreatedAt(), entity.getUpdatedAt());
	}
	
}
