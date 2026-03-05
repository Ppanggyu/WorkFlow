package com.workflow.tasks.dto;

import java.util.List;

import com.workflow.attachments.dto.AttachmentsDTO;

public record TaskSelectedRes(
		TaskDTO task,
		List<AttachmentsDTO> taskAtList
		) {

}
