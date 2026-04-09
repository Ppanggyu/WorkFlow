package com.workflow.attachment.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachment.dto.AttachmentDeleteRequest;
import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.dto.DownloadInfo;
import com.workflow.attachment.service.AttachmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AttachmentController {

	private final AttachmentService attachmentService;

	// 첨부파일 업로드 (특정 taskId에 귀속)
	@PostMapping(value = "/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> upload(@PathVariable("taskId") Long taskId,
			@RequestParam("files") List<MultipartFile> files, Authentication auth,
			@RequestParam("reason") String reason,
			@RequestParam("groupUuid") String groupUuid,
			@RequestParam("isEdit") String isEdit) {
		
		// 현재 로그인한 사용자의 ID 추출
		Long uploaderId = Long.valueOf(auth.getName());

		// AttachmentService에서 실제 파일 저장 처리 후 결과 반환
		return ResponseEntity.ok(attachmentService.uploadToTask(taskId, uploaderId, files, reason, groupUuid, isEdit));
	}

	// 첨부파일 삭제 (soft delete)
	@DeleteMapping("/attachments/delete")
	public ResponseEntity<?> delete(@RequestBody AttachmentDeleteRequest att, Authentication auth) {
		

		List<AttachmentResponse> attachment = att.attachment();
		
		Long requesterId = Long.valueOf(auth.getName());

		attachmentService.softDelete(attachment, requesterId, att.uuid(), att.reason());

		return ResponseEntity.ok().build();
	}

	// 첨부파일 다운로드
    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> download(@PathVariable("attachmentId") Long attachmentId,
                                             Authentication auth) throws Exception {

        Long requesterId = Long.valueOf(auth.getName());

        // 다운로드 정보 가져오기 (경로, 원본 이름, MIME 타입 등)
        DownloadInfo info = attachmentService.getDownloadInfo(attachmentId, requesterId);

        Resource resource = new UrlResource(info.filePath().toUri());

        if (!resource.exists() || !resource.isReadable()) {
            // 파일이 존재하지 않으면 404 반환
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // 파일명 처리 메서드
        ContentDisposition disposition = ContentDisposition
                .attachment()
                .filename(info.originalFilename(), StandardCharsets.UTF_8)
                .build();

        // Content-Type 처리
        MediaType contentType = MediaTypeFactory
                .getMediaType(info.originalFilename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        // ResponseEntity 반환: 파일 바디 + Content-Disposition + Content-Type
        return ResponseEntity.ok()
                .contentType(contentType)
                // 브라우저 다운로드 진행률 표시, 일부 클라이언트 호환성이 좋아진다.
                .contentLength(resource.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }
}
