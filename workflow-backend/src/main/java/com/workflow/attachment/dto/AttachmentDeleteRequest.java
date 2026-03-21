package com.workflow.attachment.dto;

import java.util.List;

public record AttachmentDeleteRequest(
		List<AttachmentResponse> attachment,
		String uuid,
		String reason
		) {

}
