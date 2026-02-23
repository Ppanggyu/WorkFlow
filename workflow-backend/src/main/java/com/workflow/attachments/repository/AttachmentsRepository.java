package com.workflow.attachments.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.attachments.entity.AttachmentsEntity;
import com.workflow.tasks.view.TasksView;

public interface AttachmentsRepository extends JpaRepository<AttachmentsEntity, Long>{
	
	List<AttachmentsEntity> findByTaskId_Id(Long taskId);

}
