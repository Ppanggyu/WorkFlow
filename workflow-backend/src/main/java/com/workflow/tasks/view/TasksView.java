package com.workflow.tasks.view;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.workflow.department.view.DepartmentView;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.user.view.UserView;

public interface TasksView {
	
	Long getId();
	String getTitle();
    String getDescription();
    Status getStatus();
    String getPriority();
    Visibility getVisibility();
    LocalDate getDueDate();
    String getHoldReason();
    String getCancelReason();
    Boolean getIsDeleted();
    UserView getCreatedBy();
    UserView getAssigneeId();
    DepartmentView getOwnerDepartmentId();
    DepartmentView getWorkDepartmentId();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

}
