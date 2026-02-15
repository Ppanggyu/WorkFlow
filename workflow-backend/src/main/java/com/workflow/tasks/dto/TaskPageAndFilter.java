package com.workflow.tasks.dto;

import com.workflow.tasks.enums.Status;

public record TaskPageAndFilter(
		int page,
		int size,
		String filter,
		Status status) {

}
