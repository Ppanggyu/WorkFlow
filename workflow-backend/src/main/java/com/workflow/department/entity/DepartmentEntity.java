package com.workflow.department.entity;

import java.time.LocalDateTime;
import java.util.Optional;

import com.workflow.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name="department")
@AllArgsConstructor
// (access = AccessLevel.PROTECTED) =
// 파리미터 없는 기본 생성자를 protected 접근제어자로 만들겠다. 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartmentEntity {
	
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(unique = true, nullable = false)
	private String name;
	
	@Column(unique = true, nullable = false)
	private String code;
	
	@Column(name="created_at", nullable = false)
	private LocalDateTime createdAt;
	
	@Column(name="updated_at", nullable = false)
	private LocalDateTime updatedAt;
	
	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist(){
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	// 회원 정보 수정할때만 바끼도록 수정만들때 거기에 직접 대입하기.
	// 수정될 때 : UPDATE 직전
	@PreUpdate
	void preUpdate(){
		this.updatedAt = LocalDateTime.now();
	}

}
