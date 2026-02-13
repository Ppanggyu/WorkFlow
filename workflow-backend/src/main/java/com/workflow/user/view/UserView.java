package com.workflow.user.view;

import com.workflow.user.enums.Role;
import com.workflow.user.enums.UserStatus;

public interface UserView {
	
	Long getId();
	String getEmail();
	String getName();
	String getPosition();
	Role getRole();
	UserStatus getStatus();

}
