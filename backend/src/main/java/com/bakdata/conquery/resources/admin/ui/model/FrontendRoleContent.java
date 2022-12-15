package com.bakdata.conquery.resources.admin.ui.model;

import java.util.List;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class FrontendRoleContent extends FrontendPermissionOwnerContent<Role> {

	private List<User> users;
	private List<Group> groups;
}