package com.workflow.tasks.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.UnauthorizedException;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.entity.TasksEntity;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.tasks.view.TasksView;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
//final이거나 @NonNull이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Transactional
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserRepository userRepository;

	public List<TasksView> tasks(Long id, String filter) {
		List<TasksView> taskList = null;
		UserEntity user = userRepository.findById(id)
				.orElseThrow(() -> new UnauthorizedException("오류"));

		switch (filter != null ? filter : "all") {
		case "company":
			taskList = taskRepository.findByIsDeletedFalseAndVisibility(Visibility.PUBLIC);
			break;
		case "myDepartment":
			taskList = taskRepository.findByIsDeletedFalseAndWorkDepartmentId(user.getDepartmentId());
			break;
		case "create":
			taskList = taskRepository.findTasksByIsDeletedFalseAndCreatedById(id);
			break;
		case "assignee":
			taskList = taskRepository.findByIsDeletedFalseAndAssigneeId(user);
			break;
		case "all":
		default:
			taskList = taskRepository.findAllByIsDeletedFalse();
			break;
		}

		return taskList;
	}

	public void taskForm(TaskCreateRequestDTO taskCreateRequestDTO, Long userId) {

		// 작성자 조회
		UserEntity creator = userRepository.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));

		// 담당자 조회
		UserEntity assignee = userRepository.findById(taskCreateRequestDTO.assigneeId())
				.orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다."));

		// 부서 조회
		DepartmentEntity createrDepartment = creator.getDepartmentId();

		DepartmentEntity assigneeDepartment = assignee.getDepartmentId();

		// Entity 생성
		TasksEntity task = TasksEntity.builder().title(taskCreateRequestDTO.title())
				.description(taskCreateRequestDTO.description()).status(Status.TODO) // 생성 시 기본값
				.priority(taskCreateRequestDTO.priority()).visibility(taskCreateRequestDTO.visibility())
				.dueDate(taskCreateRequestDTO.dueDate()).isDeleted(false).createdBy(creator).assigneeId(assignee)
				.ownerDepartmentId(createrDepartment).workDepartmentId(assigneeDepartment).build();

		taskRepository.save(task);

	}

}
