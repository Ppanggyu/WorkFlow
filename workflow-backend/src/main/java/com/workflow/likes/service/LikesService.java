package com.workflow.likes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.likes.entity.LikesEntity;
import com.workflow.likes.repository.LikesRepository;
import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LikesService {
	
	private final LikesRepository likesRepository;
	private final UserRepository userRepository;
	private final TaskRepository taskRepository;
	
	public void onLike(Long taskId, Long userId) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
		TaskEntity task = taskRepository.findByIdAndIsDeletedFalse(taskId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
		
		LikesEntity onLike = LikesEntity.builder()
				.userId(user)
				.taskId(task)
				.build();
		
		likesRepository.save(onLike);
	}
	
	public void delLike(Long taskId, Long userId) {
		UserEntity user = userRepository.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
		TaskEntity task = taskRepository.findByIdAndIsDeletedFalse(taskId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "업무를 찾을 수 없습니다."));
		
		likesRepository.deleteByUserId_IdAndTaskId_Id(userId, taskId);
		
	}

}
