package com.bakdata.conquery.resources.admin;

import java.io.IOException;
import java.util.Set;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;

/**
 * Enforces that the requesting subject has the admin permission.
 */
public class AdminPermissionFilter implements ContainerRequestFilter, Authorized {
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		final Subject subject = (Subject) requestContext.getSecurityContext().getUserPrincipal();

		if (subject == null) {
			throw new NotAuthorizedException("No subject was provided");
		}

		subject.authorize(this, Ability.READ);

	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return AdminPermission.onDomain(abilities);
	}
}
