package com.workflow.attachments.entity;

import java.time.LocalDateTime;

import com.workflow.tasks.entity.TasksEntity;
import com.workflow.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
@Table(name = "attachments")
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class AttachmentsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "task_id") // FK
    TasksEntity taskId;
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploader_id") // FK
    UserEntity uploaderId;
    
    String originalFilename;
    String storedFilename;
    String contentType;
    Long sizeBytes;
    String storagePath;
    boolean isDeleted;
    
    @Column(name="deleted_at")
    LocalDateTime deletedAt;
    
    @Column(name="created_at", updatable=false, nullable = false)
    LocalDateTime createdAt;
    
	// 자동 시간 세팅
	// 처음 생성 시 : INSERT 직전
	@PrePersist
	void prePersist() {
		this.createdAt = LocalDateTime.now();
	}
    

}
