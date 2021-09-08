package com.bakdata.conquery.models.auth.basic;

import com.bakdata.conquery.apiv1.auth.ProtoUser;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.rest.UserAuthenticationManagementResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
		final Optional<User> optUser = pUser.getUser(storage,false);
		if (optUser.isEmpty()){
			log.warn("Unable to add new user {}. Probably already existed.", pUser);
			return false;
		}
		final User user = optUser.get();
		final UserId id = user.getId();
		log.trace("Added the user {} to the authorization storage", id);
		if(ProtoUser.registerForAuthentication(realm, user, pUser.getCredentials(),false)) {
			log.trace("Added the user {} to the realm {}", id, realm.getName());
			return true;
		}
		log.trace("Failed to add added the user {} to the realm {}", id, realm.getName());
		return false;
	}

	public boolean updateUser(ProtoUser pUser) {
		final Optional<User> user = pUser.getUser(storage, true);
		return user.map(u -> ProtoUser.registerForAuthentication(realm, u,pUser.getCredentials(),false)).orElse(false);
	}

	public void remove(User user) {
		realm.removeUser(user);
	}

}
