package com.workflow.user.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.ErrorCode;
import com.workflow.tasks.dto.TaskResponse;
import com.workflow.user.dto.UserSimpleResponse;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.enums.Role;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
	
	private final UserRepository userRepository;
	
	public List<UserSimpleResponse> list(Long user){
		
		UserEntity loginUser = userRepository.findById(user)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "너 사용자 맞아?"));
		
		boolean isAdmin = loginUser.getRole() == Role.ADMIN;
		boolean isManager = loginUser.getRole() == Role.MANAGER;
		
		List<UserSimpleResponse> list = userRepository.findAllWithDepartment()
				.stream()
				// SYSTEM 계정 제외
				.filter(u -> u.getId() != 0)
				// ADMIN or MANAGER 일 때만 ADMIN 계정 보이게
				.filter(u -> isAdmin || isManager || u.getRole() != Role.ADMIN)
				// DTO로 변환: id, 이름, 부서명만 반환
				.map(u -> new UserSimpleResponse(
						u.getId(),
						u.getName(),
						u.getDepartment().getName(),
						u.getRole()
						))
				.toList();

		return list;
	}
	
	public Boolean dept(Long user, TaskResponse req) {
		
		UserEntity loginUser = userRepository.findById(user)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "너 사용자 맞아?"));
		
		boolean isCreator = loginUser.getDepartment().getCode().equals(req.getCreatedByDepartmentCode());
		boolean isAssignee = req.getAssigneeId() != null ? 
				loginUser.getDepartment().getCode().equals(req.getAssigneeDepartmentCode())
				: false;
		
		return isCreator || isAssignee;
	}

}
