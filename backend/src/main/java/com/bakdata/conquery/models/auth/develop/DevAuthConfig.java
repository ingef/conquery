package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import io.dropwizard.setup.Environment;

/**
 * Default configuration for the auth system. Sets up all other default components.
 * This configuration causes that every request is handled as invoked by the super user.
 */
@CPSType(base = AuthenticationConfig.class, id = "DEVELOPMENT")
public class DevAuthConfig implements AuthenticationConfig {
		
	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, AuthorizationController controller) {
		return new DefaultInitialUserRealm();
	}
}
