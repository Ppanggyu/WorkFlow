package com.workflow.audit.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.attachment.entity.AttachmentEntity;
import com.workflow.audit.dto.AuditChanges;
import com.workflow.audit.dto.AuditLogResponse;
import com.workflow.audit.entity.AuditLogEntity;
import com.workflow.audit.enums.AuditAttachmentActionType;
import com.workflow.audit.enums.AuditTaskActionType;
import com.workflow.audit.repository.AuditLogRepository;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.user.entity.UserEntity;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditLogService {
	
	private final AuditLogRepository auditLogRepository;
	
	// 업무 수정 Log 출력용
	public List<AuditLogResponse> auditLog(Long id, Long userId){
				
		List<AuditLogResponse> list = auditLogRepository.findByTaskIdOrderByCreatedAtDesc(id)
				.stream()
				.map(AuditLogResponse::from)
				.toList();
		
		return list;
	}
	
    // AuditLog 첨부파일 추가 + 삭제 + x표시 합치기 필터
    public void mergeAttachmentAudit(TaskEntity task, UserEntity user, AuditChanges change, String uuid, String reason) {

        List<AuditLogEntity> logs = auditLogRepository.findAttachmentAudits(uuid);
        
        // AuditLog 테이블의 FieldName이 Attachment인 것만 필터
        AuditLogEntity attachmentLog = logs.stream()
                .filter(l -> "Attachment".equals(l.getFieldName()))
                .findFirst()
                .orElse(null);
        
        // AuditLog 테이블의 FieldName이 x인 것만 필터
        AuditLogEntity xLog = logs.stream()
                .filter(l -> "x".equals(l.getFieldName()))
                .findFirst()
                .orElse(null);

        if (attachmentLog != null) {

            String before = attachmentLog.getBeforeValue();
            String after = attachmentLog.getAfterValue();
            
            // 기존 befor 값이 있으면 뒤에 추가
            if (change.beforeValue() != null) {
                attachmentLog.setBeforeValue(
                        before == null ? change.beforeValue() : before + ", " + change.beforeValue()
                );
            }
            
            // 기존 after 값이 있으면 뒤에 추가
            if (change.afterValue() != null) {
                attachmentLog.setAfterValue(
                        after == null ? change.afterValue() : after + ", " + change.afterValue()
                );
            }
            // 삭제 + 추가 동시 발동하면 ATTACHMENT_CHANGE or 기존 DB에 저장해둔 ATTACHMENT_XXX
            if (attachmentLog.getBeforeValue() != null && attachmentLog.getAfterValue() != null) {
                attachmentLog.setActionType(AuditAttachmentActionType.ATTACHMENT_CHANGE.name());
            } else {
                attachmentLog.setActionType(change.attachmentAT().name());
            }

            attachmentLog.setReason(reason);

        } else {

            attachmentLog = AuditLogEntity.builder()
                    .task(task)
                    .actor(user)
                    .actionType(change.attachmentAT().name())
                    .fieldName("Attachment")
                    .beforeValue(change.beforeValue())
                    .afterValue(change.afterValue())
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .updateGroupId(uuid)
                    .build();

            auditLogRepository.save(attachmentLog);
        }
        
        // fieldName 이 x인 것 삭제
        if (xLog != null) {
            auditLogRepository.delete(xLog);
        }
    }
    
    // 업무 삭제 로그
    public void taskDelete(TaskEntity task, UserEntity user, String reason) {
    	
    	AuditLogEntity deleteLog = AuditLogEntity.builder()
    			.task(task)
    			.actor(user)
    			.actionType(AuditTaskActionType.TASK_DELETED.toString())
    			.createdAt(LocalDateTime.now())
    			.updateGroupId(UUID.randomUUID().toString())
    			.reason(reason)
    			.fieldName(AuditTaskActionType.TASK_DELETED.toString())
    			.beforeValue("TaskId:"+task.getId()+"/제목:"+task.getTitle())
    			.afterValue(AuditTaskActionType.TASK_DELETED.toString())
    			.build();
    	
    	auditLogRepository.save(deleteLog);
    			
    }
    
    // 업무 복구 로그
    public void taskResotre(TaskEntity task, UserEntity user, List<AttachmentEntity> list, String reason) {
    	
    	String attList = list.isEmpty() ? "" : 
    		"/복구파일수:" + list.size() + "/복구파일:" + list.stream()
    		.map(AttachmentEntity::getStoredFilename)
    		.collect(Collectors.joining(", "));
    	
    	
    	AuditLogEntity resotreLog = AuditLogEntity.builder()
    			.task(task)
    			.actor(user)
    			.actionType(AuditTaskActionType.TASK_RESOTRE.toString())
    			.createdAt(LocalDateTime.now())
    			.updateGroupId(UUID.randomUUID().toString())
    			.reason(reason)
    			.fieldName(AuditTaskActionType.TASK_RESOTRE.toString())
    			.beforeValue(AuditTaskActionType.TASK_RESOTRE.toString())
    			.afterValue("TaskId:"+task.getId()+"/제목:"+task.getTitle() + attList != null ? attList : "")
    			.build();
    	
    	auditLogRepository.save(resotreLog);
    	
    }

}
