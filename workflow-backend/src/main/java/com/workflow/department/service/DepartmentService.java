package com.workflow.department.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.workflow.department.dto.AssigneeSelectDTO;
import com.workflow.department.repository.DepartmentRepository;
import com.workflow.tasks.enums.Priority;
import com.workflow.tasks.enums.Visibility;

import lombok.RequiredArgsConstructor;

@Service
//final이거나 @NonNull이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Transactional
public class DepartmentService {
	
	private final DepartmentRepository departmentRepository;
	
	public Map<String, List<?>> allDepartment(Collection<GrantedAuthority> role){
		List<AssigneeSelectDTO> departmentList = departmentRepository.findAllForAssigneeSelect();
		
		boolean userRole = role.stream()
				.anyMatch(auth -> auth.getAuthority().equals("ROLE_USER"));
		
		List<String> priority = Arrays.stream(Priority.values())
				.map(Enum::name)
				.collect(Collectors.toList());
		
		List<String> visibility = Arrays.stream(Visibility.values())
				.map(Enum::name)
				.collect(Collectors.toList());
		
		if(userRole) {		
			visibility.remove("PUBLIC");
		}
		
		Map<String, List<?>> map = new HashMap<>();
		map.put("allDepartment", departmentList);
		map.put("priority", priority);
		map.put("visibility", visibility);
		
		return map;
	}

}
