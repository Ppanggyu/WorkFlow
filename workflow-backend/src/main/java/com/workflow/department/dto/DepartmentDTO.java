package com.workflow.department.dto;

import com.workflow.department.entity.DepartmentEntity;

public record DepartmentDTO(
		Long id,
		String name
		){

	public static DepartmentDTO toDepartmentDto(DepartmentEntity dept) {
		if (dept == null)
			return null;

		return new DepartmentDTO(dept.getId(), dept.getName());
	}
	
}
