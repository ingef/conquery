package com.bakdata.conquery.models.auth.basic;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
import com.bakdata.conquery.util.ResourceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Business logic for the {@link UserAuthenticationManagementResource}.
 */
@RequiredArgsConstructor
@Slf4j
public class UserAuthenticationManagementProcessor {

	private final LocalAuthenticationRealm realm;
	private final MetaStorage storage;

	public boolean addUser(ProtoUser pUser) {
		// Throws an exception if it would override the existing user
		pUser.registerForAuthorization(storage, false);
		log.trace("Added the user {} to the authorization storage", pUser.getUser().getId());
		if(pUser.registerForAuthentication(realm, false)) {
			log.trace("Added the user {} to the realm {}", pUser.getUser().getId(), realm.getName());
			return true;
		}
		log.trace("Failed to add added the user {} to the realm {}", pUser.getUser().getId(), realm.getName());
		return false;
	}

	public boolean updateUser(ProtoUser pUser) {
		return pUser.registerForAuthentication(realm, false);
	}

	public void remove(UserId userId) {
		final User user = storage.getUser(userId);
		ResourceUtil.throwNotFoundIfNull(userId,user);

		realm.removeUser(user);
	}

}
