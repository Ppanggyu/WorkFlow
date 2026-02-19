package com.workflow.tasks.dto;

import java.util.List;

import org.springframework.data.domain.Page;

public record TasksResponse(
		List<TaskDTO> tasks,
		List<String> status,
		int totalPages,
		long totalElemnts
		) {

}
