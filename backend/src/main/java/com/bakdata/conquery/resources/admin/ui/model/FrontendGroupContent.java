package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Collection;

import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * This class provides the corresponding FreeMarker template with the data needed.
 */
@Getter
@SuperBuilder
public class FrontendGroupContent extends FrontendPermissionOwnerContent<Group> implements FrontendRoleOwner {

	public final Collection<FrontendUserContent> members;
	public final Collection<User> availableMembers;

	public final Collection<FrontendRoleContent> roles;
	public final Collection<Role> availableRoles;
}