package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.config.auth.AuthenticationRealmFactory;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;

import java.util.Objects;

/**
 * Default configuration for the auth system. Sets up all other default components.
 * This configuration causes that every request is handled as invoked by the super user.
 */
@CPSType(base = AuthenticationRealmFactory.class, id = "DEVELOPMENT")
public class DevAuthConfig implements AuthenticationRealmFactory {
		
	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		final MetaStorage storage = managerNode.getStorage();
		User defaultUser = managerNode.getConfig()
				.getAuthorizationRealms().getInitialUsers().get(0).getUser(storage, true).orElseThrow(() -> new IllegalStateException("There must be at least one initial user configured."));

		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(new UserIdTokenExtractor(defaultUser));

		return new DefaultInitialUserRealm(storage);
	}
}
