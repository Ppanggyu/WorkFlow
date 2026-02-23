package com.workflow.attachments.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachments.entity.AttachmentsEntity;
import com.workflow.attachments.repository.AttachmentsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentsService {

	private final AttachmentsRepository attachmentsRepository;
	private final String WIN_TEMP_DIR = "C:/WorkFlow/";

	public ResponseEntity<?> dwonloadAtt(Long userId, Long taskId, Long attachmentsId) throws Exception {

		AttachmentsEntity file = attachmentsRepository.findById(attachmentsId)
				.orElseThrow(() -> new RuntimeException("파일 없음"));

		// 로그인만 다운로드이면 굳이 필요없음
		// 임의의 주소줄로 다운로드 요청할 수 있기 때문
		// 1차 시큐리티 -> 2차 보안용
		if (!file.getTaskId().getId().equals(taskId)) {
			throw new RuntimeException("잘못된 접근");
		}
		
		Path path = Paths.get(WIN_TEMP_DIR, taskId.toString(), "file", file.getStoredFilename());

		Resource resource = new UrlResource(path.toUri());

		if (!resource.exists()) {
			throw new RuntimeException("파일이 존재하지 않습니다.");
		}

		String encodedFilename = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8).replace("+",
				"%20");

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getContentType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
				.body(resource);

	}

}
