package com.workflow.dashboard.service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.tasks.enums.Status;
import com.workflow.tasks.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardService {
	
	private final TaskRepository taskRepository;
	
	public Map<Status, Long> kpiType(Long id, String type){
		
		Map<Status, Long> status = new HashMap<>();
		
		// 내가 작성한 업무
		if(type.equals("created")) {			
			status = taskRepository.countTasksByUserIdGroupedByStatus(id)
					.stream()
					.collect(Collectors.toMap( // Status에 맞게 Map으로 변경
							row -> (Status) row[0],
							row -> (Long) row[1]));
		}
		
		// 담당 업무
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
