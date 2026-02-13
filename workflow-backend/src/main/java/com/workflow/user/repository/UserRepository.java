package com.workflow.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.user.dto.UserDTO;
import com.workflow.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
	
	// 조회
	// Optional: null일 수도 있는 값을 감싸서, null 처리 강제하는 컨테이너
	// if (user == null) {
    // ...
	// }
	Optional<UserEntity> findByEmail(String email);
	
	Optional<UserDTO> findDTOById(Long id);

	Optional<UserEntity> findById(Long id);
	
}
