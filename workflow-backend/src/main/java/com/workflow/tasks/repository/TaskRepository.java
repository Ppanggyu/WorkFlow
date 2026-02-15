package com.workflow.tasks.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.entity.TasksEntity;
import com.workflow.tasks.enums.Visibility;
import com.workflow.tasks.view.TasksView;
import com.workflow.user.entity.UserEntity;

public interface TaskRepository extends JpaRepository<TasksEntity, Long> {
	
	Page<TasksView> findByIsDeletedFalseAndVisibility(Visibility visibility, Pageable pageable);
	
	Page<TasksView> findByIsDeletedFalseAndWorkDepartmentId(DepartmentEntity departmentEntity, Pageable pageable);

	Page<TasksView> findAllByIsDeletedFalse(Pageable pageable);
	
	Page<TasksView> findTasksByIsDeletedFalseAndCreatedById(Long id, Pageable pageable);
	
	Page<TasksView> findByIsDeletedFalseAndAssigneeId(UserEntity user, Pageable pageable);
	
	Optional<TasksView> findProjectedById(Long taskId);

}
