package com.bakdata.conquery.models.config.auth;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.oidc.passwordflow.IdpDelegatingAccessTokenCreator;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Realm that supports the Open ID Connect Resource-Owner-Password-Credential-Flow with a Keycloak IdP.
 */
@Slf4j
@CPSType(base = AuthenticationRealmFactory.class, id = "OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION")
public class OIDCResourceOwnerPasswordCredentialRealmFactory implements AuthenticationRealmFactory {

	@Getter
	@Setter
	private IntrospectionDelegatingRealmFactory client;



	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, ConqueryConfig config, AuthorizationController authorizationController) {

		// Register processor that does the actual exchange of user credentials for an access token
		IdpDelegatingAccessTokenCreator idpDelegatingAccessTokenCreator = new IdpDelegatingAccessTokenCreator(client);

		// Register resources for users to exchange username and password for an access token
		registerAdminUnprotectedAuthenticationResources(authorizationController.getUnprotectedAuthAdmin(), idpDelegatingAccessTokenCreator);
		registerApiUnprotectedAuthenticationResources(authorizationController.getUnprotectedAuthApi(), idpDelegatingAccessTokenCreator);

		return client.createRealm(environment, authorizationController);
	}

	public void registerAdminUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig, IdpDelegatingAccessTokenCreator idpDelegatingAccessTokenCreator) {
		jerseyConfig.register(new TokenResource(idpDelegatingAccessTokenCreator));
		jerseyConfig.register(LoginResource.class);
	}

	public void registerApiUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig, IdpDelegatingAccessTokenCreator idpDelegatingAccessTokenCreator) {
		jerseyConfig.register(new TokenResource(idpDelegatingAccessTokenCreator));
	}

}
