package com.bakdata.conquery.models.auth.oidc.passwordflow;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import io.dropwizard.setup.Environment;
import org.keycloak.authorization.client.Configuration;

@CPSType(base = AuthenticationConfig.class, id = "OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION")
public class OIDCResourceOwnerPasswordCredentialRealmFactory extends Configuration implements AuthenticationConfig {

	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, ManagerNode manager) {
		environment.getAdminContext().
		return new OIDCResourceOwnerPasswordCredentialRealm(manager.getStorage(), this);
	}
}
