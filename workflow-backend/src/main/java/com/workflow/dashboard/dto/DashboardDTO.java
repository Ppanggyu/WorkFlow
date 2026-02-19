package com.workflow.dashboard.dto;

import java.util.Map;

import com.workflow.tasks.enums.Status;

public record DashboardDTO(
		String type,
		Map<Status, Integer> statusCount
		) {

}
