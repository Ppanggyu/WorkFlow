package com.workflow.tasks.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.enums.Priority;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tasks")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class TasksEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="title", nullable = false)
	private String title;
	
	@Column(name="description", columnDefinition = "text", nullable = false)
	private String description;
	
	@Enumerated(EnumType.STRING)
	@Column(name="status", nullable = false)
	private Status status;
	
	@Enumerated(EnumType.STRING)
	@Column(name="priority", nullable = false)
	private Priority priority;
	
	@Enumerated(EnumType.STRING)
	@Column(name="visibility", nullable = false)
	private Visibility visibility;
	
	private LocalDate dueDate;
	private String holdReason;
	private String cancelReason;
	
	@Column(name="is_deleted", nullable = false)
	private Boolean isDeleted;
	
	private LocalDateTime deletedAt;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by") // FK
	private UserEntity createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id") // FK
	private UserEntity assigneeId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_department_id") // FK
	private DepartmentEntity ownerDepartmentId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "work_department_id") // FK
	private DepartmentEntity workDepartmentId;
	
	@Column(name="created_at", nullable = false)
	private LocalDateTime createdAt;
	
	@Column(name="updated_at", nullable = false)
	private LocalDateTime updatedAt;

	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}
	
	// UPDATE 직전
	@PreUpdate
	void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

}
