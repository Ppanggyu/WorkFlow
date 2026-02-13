package com.workflow.department.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.workflow.department.dto.AssigneeSelectDTO;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.user.entity.UserEntity;

@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Long> {

	@Query("""
			select new com.workflow.department.dto.AssigneeSelectDTO(
			    u.id,
			    u.name,
			    d.name
			)
			from UserEntity u
			left join u.departmentId d

			""")
	List<AssigneeSelectDTO> findAllForAssigneeSelect();

}
