package com.workflow.department.dto;

import com.workflow.user.dto.UserDTO;

public record AssigneeSelectDTO(
		Long id, 
		String name, 
		String departmentName
		) {

}
