package com.workflow.common.job;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.entity.AttachmentEntity;
import com.workflow.attachment.repository.AttachmentRepository;
import com.workflow.audit.entity.AuditLogEntity;
import com.workflow.audit.enums.AuditAutoCleanType;
import com.workflow.audit.repository.AuditLogRepository;
import com.workflow.common.file.FileStorageService;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AutoCleanupJob {
	
	private final AuditLogRepository auditLogtRepository;
	private final TaskRepository taskRepository;
	private final AttachmentRepository attachmentRepository; // DB 접근용
	private final FileStorageService fileStorageService; // 파일 시스템 접근용

	@Value("${app.attachment-cleanup.enabled:true}")
	private boolean enabled; // 청소 기능 활성 여부

//    @Value("${app.attachment-cleanup.retention-days:1}")
	@Value("${app.attachment-cleanup.retention-minutes:10}")
	private int retentionDays; // soft delete 후 보관 일수

	// 매일 새벽 3시 실행(cron = "0 0 3 * * *")
	// fixedRate: 이전 시작 시점 기준 10분마다 실행
	// fixedDelay: 이전 종료 시점 기준 10분마다 실행
	// fixedDelay = 10000 10초마다 실행
	// fixedDelay = 60000 1분
	// fixedDelay = 600000 10분
	@Scheduled(fixedDelay = 600000) // 10분마다 실행
	@Transactional
	public void cleanupDeletedAttachments() {

		System.out.println("Cleanup 실행");

		if (!enabled)
			return; // 기능 비활성화 시 바로 종료

		// 초단위
//        Long sec = 30L;
//        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(sec);

		// 분단위 테스트용
//        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(retentionDays);

		// day 기준
		LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
		// retentionDays 이전 삭제된 첨부만 대상

		List<TaskEntity> taskTargets = taskRepository.findCleanupTargets(cutoff);
		List<AttachmentEntity> targets = attachmentRepository.findCleanupTargets(cutoff);
		// soft delete 된 파일 중 retentionDays 지난 파일 조회

		if (!targets.isEmpty()) {
			deleteAttachment(targets);
		}
		if (!taskTargets.isEmpty()) {
			deleteTaskAndAttachment(taskTargets);
		}
	}

	private void deleteAttachment(List<AttachmentEntity> targets) {
		for (AttachmentEntity a : targets) {
			// storagePath: "/uploads/...." 형태
			try {
				Path p = fileStorageService.resolveUploadPath(a.getStoragePath());
				// 실제 파일 시스템 경로 변환

				Files.deleteIfExists(p);
				// 파일 존재하면 삭제, 없으면 그냥 넘어감

				// (선택) 폴더 비었으면 정리하고 싶다면 여기에 디렉토리 정리 로직 추가 가능

			} catch (Exception ignored) {
				// 파일 삭제 실패해도 DB는 지우지 않는 편이 안전함
				// (원하면 로그 찍기)
				continue;
			}


			// 파일 삭제 로그
			AuditLogEntity log = AuditLogEntity.builder()
					.task(TaskEntity.builder().id(a.getTaskId()).build())
					.actor(UserEntity.builder().id(0L).build())
					.actionType(AuditAutoCleanType.AUTO_DELTE.toString())
					.fieldName("Attachment")
					.reason(AuditAutoCleanType.AUTO_DELTE.toString())
					.createdAt(LocalDateTime.now())
					.updateGroupId(UUID.randomUUID().toString())
					.beforeValue("연결된Task:" + a.getTaskId() + "/파일명:" + a.getStoredFilename() + "/크기:" + a.getSizeBytes())
					.build();
			auditLogtRepository.save(log);
			// 파일 삭제 성공/파일 없음 → DB row 물리 삭제
			attachmentRepository.hardDeleteById(a.getId());
			// DB에서 실제 row 제거
		}
	}

	private void deleteTaskAndAttachment(List<TaskEntity> taskTargets) {
		
		List<Long> taskIds = taskTargets.stream()
				.map(TaskEntity::getId)
				.toList();
		
		List<AttachmentEntity> attList = attachmentRepository.findByTaskIdInAndIsDeletedTrue(taskIds);
		for (AttachmentEntity att : attList) {

			// 파일 삭제 로그
			AuditLogEntity log = AuditLogEntity.builder()
					.task(TaskEntity.builder().id(att.getTaskId()).build())
					.actor(UserEntity.builder().id(0L).build())
					.actionType(AuditAutoCleanType.AUTO_DELTE.toString())
					.fieldName("Attachment")
					.reason(AuditAutoCleanType.AUTO_DELTE.toString())
					.createdAt(LocalDateTime.now())
					.updateGroupId(UUID.randomUUID().toString())
					.beforeValue("연결되었던TaskId:" + att.getTaskId() + "/파일명:" + att.getStoredFilename() + "/크기:" + att.getSizeBytes())
					.build();
			auditLogtRepository.save(log);
			// 파일 삭제 성공/파일 없음 → DB row 물리 삭제
			attachmentRepository.hardDeleteById(att.getId());
			// DB에서 실제 row 제거
		}

		for (TaskEntity t : taskTargets) {
			try {
				Path taskDir = Paths.get("C:\\WorkFlow\\uploads\\tasks", String.valueOf(t.getId()));
				if(Files.exists(taskDir)) {
					Files.walk(taskDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
				}

			} catch (Exception ignored) {
				continue;
			}

			// 파일 삭제 성공/파일 없음 → DB row 물리 삭제
			// 파일 삭제 로그
			AuditLogEntity log = AuditLogEntity.builder()
					.task(t)
					.actor(UserEntity.builder().id(0L).build())
					.actionType(AuditAutoCleanType.AUTO_DELTE.toString())
					.fieldName("Task")
					.reason(AuditAutoCleanType.AUTO_DELTE.toString())
					.createdAt(LocalDateTime.now())
					.updateGroupId(UUID.randomUUID().toString())
					.beforeValue("TaskId:"+t.getId()+"/제목:" + t.getTitle())
					.build();
			auditLogtRepository.save(log);
			taskRepository.hardDeleteById(t.getId());
		}
	}
}
