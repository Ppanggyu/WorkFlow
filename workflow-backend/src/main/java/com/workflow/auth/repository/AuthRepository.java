package com.workflow.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.workflow.auth.entity.AuthEntity;
import com.workflow.user.entity.UserEntity;

public interface AuthRepository extends JpaRepository<AuthEntity, Long>{
	
	Optional<AuthEntity> findByTokenHashAndRevokedAtIsNull(String tokenHash);
	Optional<AuthEntity> findByUser(UserEntity user);
	Optional<AuthEntity> findByUserAndRevokedAtIsNull(UserEntity user);

}
