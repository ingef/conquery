package com.bakdata.conquery.resources.hierarchies;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

import java.util.Collections;
import java.util.Set;

/**
 * This class ensures that all users have the admin permission in order to
 * access admin resources.
 */
public abstract class HAdmin extends HAuthorized implements Authorized {

	
	@Override
	@PostConstruct
	public void init() {
		super.init();

		user.authorize(this, Ability.READ);
	}

	@Override
	public ConqueryPermission createPermission(Set<Ability> abilities) {
		return AdminPermission.onDomain(abilities);
	}
}
