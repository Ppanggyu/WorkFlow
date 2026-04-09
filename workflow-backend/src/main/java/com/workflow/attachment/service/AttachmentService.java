package com.workflow.attachment.service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachment.dto.AttachmentResponse;
import com.workflow.attachment.dto.DownloadInfo;
import com.workflow.attachment.entity.AttachmentEntity;
import com.workflow.attachment.mapper.AttachmentMapper; // 새로 추가
import com.workflow.attachment.repository.AttachmentRepository;
import com.workflow.audit.dto.AuditChanges;
import com.workflow.audit.repository.AuditLogRepository;
import com.workflow.audit.service.AuditLogService;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.common.file.FileStorageService;
import com.workflow.common.file.StoredAttachment;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // 첨부파일 제한 상수
    private static final int MAX_FILES = 10;
    private static final long MAX_TOTAL_SIZE = 50L * 1024 * 1024; // 50MB

    // Task 상세 조회 시 첨부 포함용
    @Transactional(readOnly = true)
    public List<AttachmentResponse> listByTask(Long taskId) {
        // taskId 기준 soft delete 안 된 첨부만 조회
        return attachmentRepository.findByTaskIdAndIsDeletedFalseOrderByIdDesc(taskId)
                .stream()
                .map(AttachmentMapper::toResponse) // Mapper로 변환
                .toList();
    }

    // 첨부 업로드 (taskId에 귀속)
    public List<AttachmentResponse> uploadToTask(Long taskId, Long uploaderId, List<MultipartFile> files, String reason, String groupUuid, String isEdit) {

        TaskEntity task = taskRepository.findById(taskId)
        		.orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "잘못된 taskId"));
        
        UserEntity user = userRepository.findById(uploaderId)
        		.orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다."));

        if (files == null || files.isEmpty()) return List.of(); // 업로드 파일 없으면 빈 리스트

        if (files.size() > MAX_FILES) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "첨부파일은 최대 " + MAX_FILES + "개까지 가능합니다.");
        }

        long total = 0;
        for (MultipartFile f : files) if (f != null) total += f.getSize();
        if (total > MAX_TOTAL_SIZE) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "첨부파일 총합은 50MB 이하만 가능합니다.");
        }

        List<AttachmentResponse> out = new ArrayList<>();
        List<AttachmentEntity> dbList = attachmentRepository.findByTaskIdAndIsDeletedFalseOrderByIdDesc(taskId);
        List<AttachmentEntity> afterList = new ArrayList<>(dbList);
        
        for (MultipartFile f : files) {
            // 실제 저장 (task별 폴더)
            StoredAttachment stored = fileStorageService.storeTaskAttachmentToTaskDir(f, "tasks", taskId);

            // DB 엔티티 생성
            AttachmentEntity a = new AttachmentEntity();
            a.setTaskId(taskId);
            a.setUploaderId(uploaderId);
            a.setOriginalFilename(stored.originalFilename());
            a.setStoredFilename(stored.storedFilename());
            a.setContentType(stored.contentType());
            a.setSizeBytes(stored.sizeBytes());
            a.setStoragePath(stored.storagePath());
            a.setDeleted(false);
            a.setCreatedAt(LocalDateTime.now());
            
            afterList.add(a);

            // DB 저장 후 응답 변환
            AttachmentEntity saved = attachmentRepository.save(a);
            out.add(AttachmentMapper.toResponse(saved)); // Mapper 사용
        }
    	
        
        boolean edit = Boolean.parseBoolean(isEdit);
        if(edit) {
        	AuditChanges aud = AuditChanges.from(dbList, afterList);
        	
        	if(aud != null) {
        		auditLogService.mergeAttachmentAudit(task, user, aud, groupUuid, reason);
        	}        	
        }

        return out;
    }

    // soft delete 처리
    public void softDelete(List<AttachmentResponse> attachment, Long requesterId, String uuid, String reason) {
    	
    	// 삭제할 것들 id
    	List<Long> attachmentId = attachment.stream()
    			.map(AttachmentResponse::id)
    			.toList();
    	
    	// 삭제할 것들 조회
    	List<AttachmentEntity> deleteList = attachmentRepository.findByIdInAndIsDeletedFalse(attachmentId);

        TaskEntity task = taskRepository.findById(deleteList.get(0).getTaskId())
        		.orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "잘못된 taskId"));
        
        UserEntity user = userRepository.findById(requesterId)
        		.orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST, "잘못된 user"));
        
        // task 연결된 attachment 전체 조회
        List<AttachmentEntity> beforeList = attachmentRepository.findByTaskIdAndIsDeletedFalseOrderByIdDesc(task.getId());
        
        if(deleteList.isEmpty()) {
        	throw new ApiException(ErrorCode.NOT_FOUND, "첨부파일이 없습니다.");
        }
        
        // 사용자 삭제 권한 확인 (작성자인가? 담당자인가? ADMIN인가?)
        boolean isCreator = task.getCreatedBy().equals(user);
        boolean isAdmin = user.getRole().equals(Role.ADMIN);
        boolean isAssignee = Objects.equals(
        		task.getAssignee() != null ? task.getAssignee().getId() : null
    			, user.getId()) ;
        
        if (!(isCreator || isAdmin || isAssignee)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "삭제 권한이 없습니다.");
        }
        
        deleteList.forEach(a -> {
        	a.setDeleted(true);
        	a.setDeletedAt(LocalDateTime.now());
        });
        
        // 거르기 위한 조건
        Set<Long> deleteIds = deleteList.stream()
        		.map(AttachmentEntity::getId)
        		.collect(Collectors.toSet());
        
        // 거르기
        List<AttachmentEntity> afterList = beforeList.stream()
        		.filter(a -> !deleteIds.contains(a.getId()))
        		.toList();
        
        AuditChanges change = AuditChanges.from(beforeList, afterList);
        
        if(change != null) {
        	auditLogService.mergeAttachmentAudit(task, user, change, uuid, reason);
        }
        
    }

    // 다운로드용: 엔티티 + 실제 디스크 경로 반환
    @Transactional(readOnly = true)
    public DownloadInfo getDownloadInfo(Long attachmentId, Long requesterId) {

        AttachmentEntity a = attachmentRepository.findByIdAndIsDeletedFalse(attachmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "첨부파일이 없습니다."));

        Path filePath = fileStorageService.resolveUploadPath(a.getStoragePath());

        return new DownloadInfo(
                a.getOriginalFilename(),
                a.getContentType(),
                filePath
        );
    }

    // Task별 남아있는 활성 첨부 파일 수 조회
    public long countActiveByTask(Long taskId) {
        return attachmentRepository.countActiveByTaskId(taskId);
    }
    
    // task 기준 attachment 논리 삭제
    public void taskDelete(TaskEntity task) {
    	
    	List<AttachmentEntity> a = attachmentRepository.findByTaskIdAndIsDeletedFalse(task.getId());
    	
    	if(a.isEmpty()) {
    		return;
    	}
    	
    	for(AttachmentEntity update : a) {
    		update.setDeleted(true);
    		update.setDeletedAt(LocalDateTime.now());
    	}
    	
    }
    
    // 논리삭제된 첨부파일 조회
    @Transactional(readOnly = true)
    public List<AttachmentEntity> deletedAttList(Long taskId) {
    	return attachmentRepository.findByTaskIdAndIsDeletedTrue(taskId);
    }
    
}
