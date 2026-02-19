package com.workflow.dashboard.service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.common.exception.UnauthorizedException;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {
	
	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	
	public Map<Status, Long> kpiType(Long id, String type){
		
		UserEntity user = userRepository.findById(id).orElseThrow(() -> new UnauthorizedException("오류"));
		Map<Status, Long> status = new HashMap();
		System.out.println("type : " + type);
		
		
		if(type.equals("created")) {			
			status = taskRepository.countTasksByUserIdGroupedByStatus(id)
					.stream()
					.collect(Collectors.toMap(
							row -> (Status) row[0],
							row -> (Long) row[1]));
		}
		
		if(type.equals("assignee")) {
			status = taskRepository.countTasksByAssigneeIdGroupedByStatus(id)
					.stream()
					.collect(Collectors.toMap(
							row -> (Status) row[0],
							row -> (Long) row[1]));
		}
		
		return status;
		
	}

}
