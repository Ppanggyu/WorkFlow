package com.workflow.attachments.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.attachments.entity.AttachmentsEntity;

public interface AttachmentsRepository extends JpaRepository<AttachmentsEntity, Long>{

}
