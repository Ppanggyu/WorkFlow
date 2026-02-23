package com.workflow.attachments.dto;

import java.time.LocalDateTime;

import com.workflow.attachments.entity.AttachmentsEntity;

public record AttachmentsDTO(Long id,
		String originalFilename,
		String contentType,
		Long sizeBytes,
		LocalDateTime createdAt,
		String formatFileSize) {

	public static AttachmentsDTO from(AttachmentsEntity entity) {
		return new AttachmentsDTO(
				entity.getId(),
				entity.getOriginalFilename(),
				entity.getContentType(),
				entity.getSizeBytes(),
				entity.getCreatedAt(),
				formatFileSize(entity.getSizeBytes())
				);
	}

	private static String formatFileSize(Long bytes) {
		if (bytes == null || bytes <= 0)
			return "0 B";

		String[] units = { "B", "KB", "MB", "GB"};

		int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));

		return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
	}

}
