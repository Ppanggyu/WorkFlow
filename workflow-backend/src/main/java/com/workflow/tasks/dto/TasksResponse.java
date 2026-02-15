package com.workflow.tasks.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.workflow.tasks.view.TasksView;

public record TasksResponse(
		Page<TasksView> tasks,
		List<String> status
		) {

}
