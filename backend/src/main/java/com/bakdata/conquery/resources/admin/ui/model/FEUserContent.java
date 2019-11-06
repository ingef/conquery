package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * This class provides the corresponding FreeMarker template with the data needed.
 */
@Getter
@SuperBuilder
public class FEUserContent extends FEPermissionOwnerContent<User>{
	public Set<Role> roles;
	public Collection<Role> availableRoles;
}