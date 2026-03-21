package com.workflow.audit.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.audit.entity.AuditLogEntity;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long>{
	
	List<AuditLogEntity> findByTaskIdOrderByCreatedAtDesc(Long taskId);
	
	@Query("""
		    select a
		    from AuditLogEntity a
		    where a.updateGroupId = :uuid
		      and a.fieldName in ('Attachment','x')
		""")
	List<AuditLogEntity> findAttachmentAudits(@Param("uuid") String uuid);

}
