package com.workflow.user.dto;

import com.workflow.user.enums.Role;
import com.workflow.user.enums.UserStatus;

public record UserDTO(
		Long id,
		String email,
		String name,
		String position,
		Role role,
		UserStatus status
		) {

}
