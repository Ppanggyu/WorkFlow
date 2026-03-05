package com.workflow.user.dto;

import com.workflow.user.entity.UserEntity;
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
	
	public static UserDTO toUserDto(UserEntity user) {
		if (user == null)
			return null;

		return new UserDTO(user.getId(), user.getEmail(), user.getName(), user.getPosition(), user.getRole(),
				user.getStatus());
	}
	
}
