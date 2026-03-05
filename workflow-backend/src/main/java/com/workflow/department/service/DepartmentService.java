package com.workflow.department.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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
	
	public Map<String, List<?>> allDepartment(@AuthenticationPrincipal User user){
		
		// JwtProvider에서 설정한 로그인한 유저 role 가져옴
		Collection<GrantedAuthority> role = user.getAuthorities();
		
		// 업무 담당자 지정 리스트
		List<AssigneeSelectDTO> departmentList = departmentRepository.findAllForAssigneeSelect();
		
		// 공개범위 CEO인지 확인하여 PUBLIC 넣을지 확인 하기 위함
		boolean userRole = role.stream()
				.anyMatch(auth -> auth.getAuthority().equals("ROLE_CEO"));
		
		// Enum을 배열로 변환
		List<String> priority = Arrays.stream(Priority.values())
				// Enum을 문자열로 변환
				.map(Enum::name)
				// 리시트로 변환
				.collect(Collectors.toList());
		
		List<String> visibility = Arrays.stream(Visibility.values())
				.map(Enum::name)
				.collect(Collectors.toList());
		
		// CEO 아니면 공개범위 PUBLIC 삭제
		if(!userRole) {		
			visibility.remove("PUBLIC");
		}
		
		Map<String, List<?>> map = new HashMap<>();
		map.put("allDepartment", departmentList);
		map.put("priority", priority);
		map.put("visibility", visibility);
		
		return map;
	}

}
