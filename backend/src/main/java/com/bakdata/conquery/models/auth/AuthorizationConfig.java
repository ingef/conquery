package com.bakdata.conquery.models.auth;

import java.util.List;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Configurations of this type define the initial users with their permissions
 * and optional credentials that might be registered by realm that are
 * {@link UserManageable}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface AuthorizationConfig {

	/**
	 * A list of initial users that are loaded on start of the application.
	 * Users that are defined in the configuration will override the users in the storage if they have the same id.
	 */
	List<ProtoUser> getInitialUsers();

	/**
	 * A list of permission scopes/domains that should be used to generate the permission overview as an CSV.
	 * Usually {@link QueryPermission} are not included, since they add little information to the overview.
	 */
	List<String> getOverviewScope();
}
