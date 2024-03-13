package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.auth.AuthenticationRealmFactory;
import io.dropwizard.core.setup.Environment;

/**
 * Default configuration for the auth system. Sets up all other default components.
 * This configuration causes that every request is handled as invoked by the super user.
 */
@CPSType(base = AuthenticationRealmFactory.class, id = "DEVELOPMENT")
public class DevAuthConfig implements AuthenticationRealmFactory {
		
	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, ConqueryConfig config, AuthorizationController authorizationController) {
		User defaultUser = config.getAuthorizationRealms()
								 .getInitialUsers()
								 .get(0)
								 .createOrOverwriteUser(authorizationController.getStorage());

		DefaultAuthFilter.registerTokenExtractor(new UserIdTokenExtractor(defaultUser), environment.jersey().getResourceConfig());

		return new DefaultInitialUserRealm(authorizationController.getStorage());
	}
}
