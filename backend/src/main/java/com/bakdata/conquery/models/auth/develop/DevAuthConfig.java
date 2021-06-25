package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;

import java.util.Objects;

/**
 * Default configuration for the auth system. Sets up all other default components.
 * This configuration causes that every request is handled as invoked by the super user.
 */
@CPSType(base = AuthenticationConfig.class, id = "DEVELOPMENT")
public class DevAuthConfig implements AuthenticationConfig {
		
	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		User defaultUser = Objects.requireNonNull(managerNode.getConfig()
				.getAuthorization().getInitialUsers().get(0).getUser(), "There must be at least one initial user configured.");

		managerNode.getAuthController().getAuthenticationFilter().registerTokenExtractor(new UserIdTokenExtractor(defaultUser));

		return new DefaultInitialUserRealm();
	}
}
