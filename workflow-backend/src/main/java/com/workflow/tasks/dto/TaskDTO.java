package com.workflow.tasks.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.workflow.department.dto.DepartmentDTO;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.user.dto.UserDTO;

public record TaskDTO(
        String title,
        String description,
        Status status,
        String priority,
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
    
	
	
}
