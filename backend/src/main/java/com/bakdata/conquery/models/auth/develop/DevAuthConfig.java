package com.bakdata.conquery.models.auth.develop;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.web.DefaultAuthFilter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.auth.AuthenticationRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import io.dropwizard.core.setup.Environment;

/**
 * Default configuration for the auth system. Sets up all other default components.
 * This configuration causes that every request is handled as invoked by the super user.
 */
@CPSType(base = AuthenticationRealmFactory.class, id = "DEVELOPMENT")
public class DevAuthConfig implements AuthenticationRealmFactory {
		
	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, ConqueryConfig config, AuthorizationController authorizationController) {

		DefaultAuthFilter.registerTokenExtractor(UserIdTokenExtractor.class, environment.jersey().getResourceConfig());
		DefaultAuthFilter.registerTokenExtractor(UserIdTokenExtractor.class, authorizationController.getAdminServlet().getJerseyConfig());
		DefaultAuthFilter.registerTokenExtractor(UserIdTokenExtractor.class, authorizationController.getAdminServlet().getJerseyConfigUI());

		// Use the first defined user als the default user. This is usually the superuser if the DevelopmentAuthorizationConfig is set
		final UserId defaultUserId = new UserId(config.getAuthorizationRealms().getInitialUsers().get(0).getName());
		return new FirstInitialUserRealm(authorizationController.getStorage(), defaultUserId);
	}
}
