package com.bakdata.conquery.resources.hierarchies;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.permissions.AdminPermission;
import com.bakdata.conquery.resources.admin.rest.AdminProcessor;

/**
 * This class ensures that all users have the admin permission in order to
 * access admin resources.
 */
public abstract class HAdmin extends HAuthorized {

	@Inject
	protected AdminProcessor processor;
	
	@PostConstruct
	public void init() {
		super.init();
		AuthorizationHelper.authorize(user, AdminPermission.onDomain());
	}
}
