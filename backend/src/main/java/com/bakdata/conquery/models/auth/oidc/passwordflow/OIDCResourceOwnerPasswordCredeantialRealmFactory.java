package com.bakdata.conquery.models.auth.oidc.passwordflow;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import org.keycloak.authorization.client.Configuration;

@CPSType(base = AuthenticationConfig.class, id = "OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION")
public class OIDCResourceOwnerPasswordCredeantialRealmFactory extends Configuration implements AuthenticationConfig {

//	private String realm;
//	@JsonProperty("auth-server-url")
//	private URL authServerUrl;
//	@JsonProperty("ssl-required")
//	private String sslRequired;
//	private String resource;
//	@JsonProperty("verify-token-audience")
//	private boolean verifyTokenAudience;
//	private Credentials credentials;
//	@JsonProperty("confidential-port")
//	private Integer confidentialPort;
//	@JsonProperty("policy-enforcer")
//	private Object policyEnforcer;

	@Override
	public ConqueryAuthenticationRealm createRealm(AuthorizationController controller) {
		return new OIDCResourceOwnerPasswordCredeantialRealm(controller.getStorage(), this);
	}

//	public static class Credentials {
//
//		private String secret;
//	}

}
