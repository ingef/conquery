package com.bakdata.conquery.models.auth.oidc.passwordflow;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

@Slf4j
@CPSType(base = AuthenticationConfig.class, id = "OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION")
public class OIDCResourceOwnerPasswordCredentialRealmFactory extends Configuration implements OIDCAuthenticationConfig {
	
	private AuthzClient authClient;
	
	@Getter
	private ClientAuthentication clientAuthentication = new ClientSecretBasic(new ClientID(getClientId()), new Secret(getClientSecret()));

	
	private AuthzClient getAuthClient(boolean exceptionOnFailedRetrieval) {
		if(authClient != null) {
			return authClient;
		}
		try {
			// This tries to contact the identity providers discovery endpoint and can possibly timeout
			AuthzClient authzClient = AuthzClient.create(this);
			clientAuthentication = new ClientSecretBasic(new ClientID(getClientId()), new Secret(getClientSecret()));
			return authzClient;
		} catch (RuntimeException e) {
			log.warn("Unable to estatblish connection to auth server.", log.isTraceEnabled()? e : null );
			if(exceptionOnFailedRetrieval) {
				throw e;
			}
		}
		return null;
	}
	
	public String getTokenEndpoint(){
		return getAuthClient(true).getServerConfiguration().getTokenEndpoint();
	}
	
	public String getIntrospectionEndpoint() {
		return getAuthClient(true).getServerConfiguration().getIntrospectionEndpoint();
	}
	
	private String getClientId() {
		return getAuthClient(true).getConfiguration().getResource();
	}
	
	private String getClientSecret() {
		return getAuthClient(true).getConfiguration().getClientKeyPassword();
	}
	
	
	public final ClientAuthentication getClientAuthentication() {

		return new ClientSecretBasic(new ClientID(getClientId()), new Secret(getClientSecret()));
	}

	@Override
	public ConqueryAuthenticationRealm createRealm(Environment environment, AuthorizationController controller) {
		this.authClient = getAuthClient(false);
		if(environment != null && environment.admin() != null) {
			environment.admin().addTask(new Task("keycloak-update-authz-client") {
				
				@Override
				public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
					authClient = getAuthClient(true);
				}
			});
		}
		return new OIDCResourceOwnerPasswordCredentialRealm(controller.getStorage(), this);
	}
}
