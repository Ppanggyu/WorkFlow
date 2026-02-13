package com.workflow.tasks.dto;

import java.time.LocalDate;

import com.workflow.tasks.enums.Priority;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;

public record TaskCreateRequestDTO(
		String title, 
		String description, 
		Status status, 
		Priority priority,
		Visibility visibility,
		LocalDate dueDate,
		boolean isDeleted,

		Long assigneeId, 
		Long ownerDepartmentId, 
		Long workDepartmentId) {

}
