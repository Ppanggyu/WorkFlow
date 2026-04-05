package com.workflow.tasks.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.workflow.tasks.entity.TaskEntity;
import com.workflow.tasks.enums.TaskStatus;
import com.workflow.user.enums.Role;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

	// @EntityGraph: 연관 엔티티를 한 번에 같이 조회하라고 강제하는 옵션(N+1 문제 방지용 + Lazy 로딩 최적화)
	// A를 가져올때 B도 같이 가져와
	// 단, 즉시

	// 기본 목록/필터
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalse(Pageable pageable); 
    // 삭제되지 않은 모든 Task를 페이지 단위로 조회, 작성자/담당자 정보를 즉시 가져옴

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndStatus(TaskStatus status, Pageable pageable);
    // 삭제되지 않은 Task 중 특정 상태(status)를 가진 것만 조회

	// created 탭: 내가 만든 것만
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndCreatedBy_Id(Long createdById, Pageable pageable);
    // 특정 사용자가 생성한 삭제되지 않은 Task 조회

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndCreatedBy_IdAndStatus(Long createdById, TaskStatus status, Pageable pageable);
    // 특정 사용자가 생성한 Task 중 상태가 특정 값인 것만 조회

	// assigned 탭: 내가 담당인 것만
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndAssignee_Id(Long assigneeId, Pageable pageable);
    // 특정 사용자가 담당인 삭제되지 않은 Task 조회
	
	// like 탭
	@Query("""
			select t
			from LikesEntity l
			left join TaskEntity t
			on t.id = l.taskId.id
			and l.userId.id = :userId
			where t.isDeleted = false
			order by l.id desc
			""")
	Page<TaskEntity> findByLikeTask(@Param("userId")Long userId, Pageable pageable);
	
	// like 탭 + status 조회
	@Query("""
			select t
			from LikesEntity l
			left join TaskEntity t
			on t.id = l.taskId.id
			and l.userId.id = :userId
			where t.isDeleted = false
			and t.status = :status
			order by l.id desc
			""")
	Page<TaskEntity> findLikeTaskWithStatus(@Param("userId")Long userId, @Param("status")TaskStatus status, Pageable pageable);
	
	// deleted 탭 : 삭제된 업무
	@Query("""
			select t
			from TaskEntity t
			where t.isDeleted = true
			and 
			(t.createdBy.id = :userId
			or t.assignee.id = :userId)
			""")
	Page<TaskEntity> findDeletedList(@Param("userId")Long userId, Pageable pageable); 
	
	// deleted 탭 + status
	@Query("""
			select t
			from TaskEntity t
			where t.isDeleted = true
			and 
			(t.createdBy.id = :userId
			or t.assignee.id = :userId)
			and t.status = :status
			""")
	Page<TaskEntity> findDeletedListWithStatus(@Param("userId")Long userId, @Param("status")TaskStatus status, Pageable pageable); 
	
	// admin 기준 deleted 탭
	Page<TaskEntity> findByIsDeletedTrue(Pageable pageable); 
	
	// admin 기준 deleted 탭 + status + dept
	@Query("""
			select t
			from TaskEntity t
			where t.isDeleted = true
			and (
			:status is null
			or t.status = :status
			)
		    and (:dept is null
			    or t.ownerDepartment.name = :dept
				or t.workDepartment.name = :dept
			)
			""")
	Page<TaskEntity> findIsDeletedTaskWithStatusAndDept(@Param("status")TaskStatus status, @Param("dept")String dept, Pageable pageable);
	
	// manager 기준 deleted 탭 팀원이 삭제한거, 자기가 작성한거, 담당자로 받은거
	@Query("""
			select t
			from TaskEntity t
			where t.isDeleted = true
			and exists (
				select 1
				from AuditLogEntity a
				where a.task = t
				and a.actor.id in :userId
				and a.actionType = 'TASK_DELETED'
			)
			""")
	Page<TaskEntity> findDeletedListWithManager(@Param("userId")List<Long> userId, Pageable pageable);
	
	// manager 기준 deleted 탭 + status
	@Query("""
			select t
			from TaskEntity t
			where t.isDeleted = true
			and t.status = :status
			and exists (
				select 1
				from AuditLogEntity a
				where a.task = t
				and a.actor.id in :userId
				and a.actionType = 'TASK_DELETED'
			)
			""")
	Page<TaskEntity> findDeletedListWithManagerAndStatus(@Param("userId")List<Long> userId, @Param("status")TaskStatus status, Pageable pageable);

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	Page<TaskEntity> findByIsDeletedFalseAndAssignee_IdAndStatus(Long assigneeId, TaskStatus status, Pageable pageable);
	

	// count 계열은 EntityGraph 붙이면 손해
	long countByIsDeletedFalseAndAssignee_IdAndStatus(Long userId, TaskStatus status);
    // 특정 사용자가 담당한 특정 상태의 Task 개수만 계산 (EntityGraph 미사용)

	// created KPI (내가 만든 업무)
    long countByIsDeletedFalseAndCreatedBy_IdAndStatus(Long userId, TaskStatus status);
    // 특정 사용자가 생성한 특정 상태 Task 개수

	// 전사 업무: PUBLIC만
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.visibility = 'PUBLIC'
	""")
	Page<TaskEntity> findPublicOnly(Pageable pageable);
    // 모든 사용자가 볼 수 있는 공개 Task 조회

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.visibility = 'PUBLIC'
		  and (
			:status is null
			or t.status = :status
	    )
		  and (:dept is null
			    or t.ownerDepartment.name = :dept
				or t.workDepartment.name = :dept
			)
	""")
	Page<TaskEntity> findPublicOnlyByStatusAndDept(@Param("status") TaskStatus status, @Param("dept") String dept, Pageable pageable);
    // 공개 Task 중 특정 상태만 조회

	// 전체 업무: 내가 볼 수 있는 모든 업무
	// PUBLIC + (DEPARTMENT면 내 부서) + (PRIVATE면 내가 작성/담당)
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
	  select t from TaskEntity t
	  where t.isDeleted = false
	    and (
			  :isAdmin = true
			  or(
		          t.createdBy.id = :userId
		       or t.assignee.id = :userId
		       or t.visibility = 'PUBLIC'
		       or (t.visibility = 'DEPARTMENT' and t.workDepartment.id = :deptId)
		       )
	    )
	""")
	Page<TaskEntity> findAllVisibleForUser(@Param("userId") Long userId,
	                                      @Param("deptId") Long deptId,
	                                      @Param("isAdmin") boolean isAdmin,
	                                      Pageable pageable);
    // 로그인 사용자가 볼 수 있는 모든 Task 조회: 공개/내부부서/내가 작성하거나 담당

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
	  select t from TaskEntity t
	  where t.isDeleted = false
		    and (:status is null or t.status = :status)
		    and (:dept is null
			    or t.ownerDepartment.name = :dept
				or t.workDepartment.name = :dept
			)
			and (
			  :isAdmin = true
		       or t.createdBy.id = :userId
		       or t.assignee.id is not null and t.assignee.id = :userId
		       or t.visibility = 'PUBLIC'
		       or (t.visibility = 'DEPARTMENT' and t.workDepartment.id = :deptId)
	    )
	""")
	Page<TaskEntity> findAllVisibleForUserByStatus(@Param("userId") Long userId,
	                                              @Param("deptId") Long deptId,
	                                              @Param("status") TaskStatus status,
	                                              @Param("dept") String dept,
	                                              @Param("isAdmin") boolean isAdmin,
	                                              Pageable pageable);
    // 로그인 사용자가 볼 수 있는 Task 중 특정 상태인 것만 조회

	// 우리팀 업무: 우리 팀만 + PRIVATE는 (작성자/담당자=나)만 예외 허용
	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.workDepartment.id = :deptId
		  and (
		        t.visibility <> 'PRIVATE'
		     or (t.visibility = 'PRIVATE' and (t.createdBy.id = :userId or t.assignee.id = :userId))
		  )
	""")
	Page<TaskEntity> findTeamVisibleForUser(@Param("userId") Long userId,
	                                       @Param("deptId") Long deptId,
	                                       Pageable pageable);
    // 내 부서 팀 Task 조회, PRIVATE인 경우 작성자/담당자만 예외 허용

	@EntityGraph(attributePaths = { "createdBy", "assignee" })
	@Query("""
		select t from TaskEntity t
		where t.isDeleted = false
		  and t.status = :status
		  and t.workDepartment.id = :deptId
		  and (
		        t.visibility <> 'PRIVATE'
		     or (t.visibility = 'PRIVATE' and (t.createdBy.id = :userId or t.assignee.id = :userId))
		  )
	""")
	Page<TaskEntity> findTeamVisibleForUserByStatus(@Param("userId") Long userId,
	                                               @Param("deptId") Long deptId,
	                                               @Param("status") TaskStatus status,
	                                               Pageable pageable);
    // 내 부서 팀 Task 중 특정 상태만 조회

	@EntityGraph(attributePaths = {
	        "createdBy", "createdBy.department",
	        "assignee", "assignee.department",
	        "ownerDepartment",
	        "workDepartment"
	})
	@Query("""
		    select t from TaskEntity t
		    where t.isDeleted = false
		      and t.id = :taskId
		      and (
		            t.createdBy.id = :userId
		         or t.assignee.id = :userId
		         or t.visibility = 'PUBLIC'
		         or (t.visibility = 'DEPARTMENT' and t.workDepartment.id = :deptId)
		      )
		""")
	Optional<TaskEntity> findDetailVisibleForUser(@Param("taskId") Long taskId,
												  @Param("userId") Long	userId,
												  @Param("deptId") Long	deptId);
    // 특정 Task 상세 조회: 로그인 사용자가 접근 가능한 Task만, 작성자/담당자/공개/부서 포함
	
	Optional<TaskEntity> findByIdAndIsDeletedFalse(Long id);
	Optional<TaskEntity> findByIdAndIsDeletedTrue(Long id);
	
	@Query("""
	        select a
	          from TaskEntity a
	         where a.isDeleted = true
	           and a.deletedAt is not null
	           and a.deletedAt < :cutoff
	         order by a.deletedAt asc
	    """)
	List<TaskEntity> findCleanupTargets(@Param("cutoff") LocalDateTime cutoff);
	
	@Modifying
    @Query("""
        delete from TaskEntity a
         where a.id = :id
    """)
    int hardDeleteById(@Param("id") Long id);
	
	Page<TaskEntity> findByAssigneeIdAndIsDeletedFalse(Long userId, Pageable pageable);
	Page<TaskEntity> findByCreatedByIdAndIsDeletedFalse(Long userId, Pageable pageable);
}
