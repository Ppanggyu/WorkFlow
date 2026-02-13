package com.workflow.tasks.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.entity.TasksEntity;
import com.workflow.tasks.enums.Visibility;
import com.workflow.tasks.view.TasksView;
import com.workflow.user.entity.UserEntity;

public interface TaskRepository extends JpaRepository<TasksEntity, Long> {
	
	List<TasksView> findByIsDeletedFalseAndVisibility(Visibility visibility);
	
	List<TasksView> findByIsDeletedFalseAndWorkDepartmentId(DepartmentEntity departmentEntity);

	List<TasksView> findAllByIsDeletedFalse();
	
	List<TasksView> findTasksByIsDeletedFalseAndCreatedById(Long id);
	
	List<TasksView> findByIsDeletedFalseAndAssigneeId(UserEntity user);

}
