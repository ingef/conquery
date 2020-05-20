package com.bakdata.conquery.models.auth;

import com.bakdata.conquery.io.cps.CPSBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface AuthenticationConfig {
	
	/**
	 * Gets the realm specified in the configuration.
	 * @param storage (Unused) A storage from which a realm can query information about subjects and permissions.
	 * @return The realm.
	 */
	@JsonIgnore
	ConqueryAuthenticationRealm createRealm(AuthorizationController controller);
}
