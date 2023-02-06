package com.bakdata.conquery.models.auth.basic;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
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

	public boolean tryRegister(ProtoUser pUser) {
		final UserId id = pUser.createId();
		User user = storage.getUser(id);
		if (user == null) {
			log.warn("Unable to add new user {}. Probably already existed.", pUser);
			return false;
		}
		log.trace("Added the user {} to the authorization storage", id);
		if(AuthorizationHelper.registerForAuthentication(realm, user, pUser.getCredentials(), false)) {
			log.trace("Added the user {} to the realm {}", id, realm.getName());
			return true;
		}
		log.trace("Failed to add added the user {} to the realm {}", id, realm.getName());
		return false;
	}

	public boolean updateUser(ProtoUser pUser) {
		final User user = pUser.createOrOverwriteUser(storage);
		AuthorizationHelper.registerForAuthentication(realm, user,pUser.getCredentials(),false);
		return true;
	}

	public void remove(User user) {
		realm.removeUser(user);
	}

}
