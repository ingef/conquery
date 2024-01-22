package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for a simple realm that just forwards tokens to the IDP for verification.
 */
@Slf4j
@CPSType(base = AuthenticationRealmFactory.class, id = "OIDC_AUTHORIZATION_CODE_FLOW")
public class OIDCAuthorizationCodeFlowRealmFactory implements AuthenticationRealmFactory {

	@Getter
	@Setter
	private IntrospectionDelegatingRealmFactory client;

	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		return client.createRealm(managerNode);
	}
}
