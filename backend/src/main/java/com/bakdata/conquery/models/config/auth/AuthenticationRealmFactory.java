package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.shiro.realm.AuthenticatingRealm;

@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
@CPSBase
public interface AuthenticationRealmFactory {
	
	/**
	 * Gets the realm specified in the configuration.
	 * @return The realm.
	 * @param managerNode
	 */
	@JsonIgnore
	ConqueryAuthenticationRealm createRealm(ManagerNode managerNode);
}
