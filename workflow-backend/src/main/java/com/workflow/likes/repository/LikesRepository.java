package com.workflow.likes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.likes.entity.LikesEntity;

public interface LikesRepository extends JpaRepository<LikesEntity, Long>{

	List<LikesEntity> findByUserId_Id(Long user);
	
	@Query("""
			select l.taskId.id
			from LikesEntity l
			where l.userId.id = :userId
			and l.taskId.id in :taskId
			""")
	List<Long> findLikedTaskIds(@Param("userId") Long userId, @Param("taskId")List<Long> taskId);
	
	boolean existsByUserId_IdAndTaskId_Id(Long userId, Long taskId);

	void deleteByUserId_IdAndTaskId_Id(Long userId, Long taskId);

}
