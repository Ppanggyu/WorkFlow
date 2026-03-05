package com.workflow.tasks.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.tasks.entity.TasksEntity;

public interface TaskRepository extends JpaRepository<TasksEntity, Long>, JpaSpecificationExecutor<TasksEntity> {
	
	Optional<TasksEntity> findById(Long taskId);
	
	@Query("SELECT t.status, COUNT(t.id)"
			+ " FROM TasksEntity t"
			+ " WHERE t.createdBy.id = :id"
			+ " GROUP BY t.status")
	List<Object[]> countTasksByUserIdGroupedByStatus(@Param("id")Long id);
	
	@Query("SELECT t.status, COUNT(t.id)"
			+ " FROM TasksEntity t"
			+ " WHERE t.assigneeId.id = :id"
			+ " GROUP BY t.status")
	List<Object[]> countTasksByAssigneeIdGroupedByStatus(@Param("id")Long id);

}
