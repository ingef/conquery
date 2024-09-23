package com.bakdata.conquery.resources.admin.ui.model;

import java.util.Collection;

import com.bakdata.conquery.models.auth.entities.Role;

/**
 * Composition interface for frontend contents that represent auth entities that
 * are able to impersonate {@link Role}s.
 */
public interface FrontendRoleOwner {

	Collection<Role> getRoles();

	Collection<Role> getAvailableRoles();
}
