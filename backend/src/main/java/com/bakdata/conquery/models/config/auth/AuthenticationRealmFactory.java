package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.core.setup.Environment;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface AuthenticationRealmFactory {
	
	/**
	 * Gets the realm specified in the configuration.
	 *
	 * @param environment
	 * @param config
	 * @param authorizationController
	 * @return The realm.
	 */
	@JsonIgnore
	ConqueryAuthenticationRealm createRealm(Environment environment, ConqueryConfig config, AuthorizationController authorizationController);
}
