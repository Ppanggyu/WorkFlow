package com.workflow.tasks.dto;

import java.util.List;

import com.workflow.attachments.dto.AttachmentsDTO;
import com.workflow.tasks.view.TasksView;

public record TaskSelectedRes(
		TasksView task,
		List<AttachmentsDTO> taskAtList
		) {

}
