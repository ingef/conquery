package com.bakdata.conquery.models.auth.oidc.passwordflow;

import static com.bakdata.conquery.models.auth.oidc.passwordflow.TokenProcessor.CONFIDENTIAL_CREDENTIAL;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.auth.AuthenticationConfig;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealm;
import com.bakdata.conquery.models.auth.oidc.OIDCAuthenticationConfig;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.validation.ValidationMethod;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

@Slf4j
@CPSType(base = AuthenticationConfig.class, id = "OIDC_RESOURCE_OWNER_PASSWORD_CREDENTIAL_AUTHENTICATION")
public class OIDCResourceOwnerPasswordCredentialRealmFactory extends Configuration implements OIDCAuthenticationConfig {
	
	private AuthzClient authClient;

	@JsonIgnore
	private AuthzClient getAuthClient(boolean exceptionOnFailedRetrieval) {
		if(authClient != null) {
			return authClient;
		}
		try {
			// This tries to contact the identity providers discovery endpoint and can possibly timeout
			AuthzClient authzClient = AuthzClient.create(this);
			return authzClient;
		} catch (RuntimeException e) {
			log.warn("Unable to estatblish connection to auth server.", log.isTraceEnabled()? e : null );
			if(exceptionOnFailedRetrieval) {
				throw e;
			}
		}
		return null;
	}
	
	@JsonIgnore
	public String getTokenEndpoint(){
		return getAuthClient(true).getServerConfiguration().getTokenEndpoint();
	}

	@JsonIgnore
	public String getIntrospectionEndpoint() {
		return getAuthClient(true).getServerConfiguration().getIntrospectionEndpoint();
	}

	@JsonIgnore
	private String getClientId() {
		return getResource();
	}

	@JsonIgnore
	private String getClientSecret() {
		return (String) credentials.get(CONFIDENTIAL_CREDENTIAL);
	}
	

	@JsonIgnore
	public final ClientAuthentication getClientAuthentication() {
		return new ClientSecretBasic(new ClientID(getClientId()), new Secret(getClientSecret()));
	}

	@Override
	public ConqueryAuthenticationRealm createRealm(ManagerNode managerNode) {
		this.authClient = getAuthClient(false);
		if(managerNode != null && managerNode.getEnvironment().admin() != null) {
			managerNode.getEnvironment().admin().addTask(new Task("keycloak-update-authz-client") {
				
				@Override
				public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
					authClient = getAuthClient(true);
				}
			});
		}

		TokenProcessor tokenProcessor = new TokenProcessor(this);
		registerAdminUnprotectedAuthenticationResources(managerNode.getUnprotectedAuthAdmin(),tokenProcessor);
		registerApiUnprotectedAuthenticationResources(managerNode.getUnprotectedAuthApi(),tokenProcessor);

		return new IntrospectionDelegatingRealm<>(managerNode.getStorage(), this);
	}

	public void registerAdminUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig, TokenProcessor tokenProcessor) {
		jerseyConfig.register(new TokenResource(tokenProcessor));
		jerseyConfig.register(LoginResource.class);
	}

	public void registerApiUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig, TokenProcessor tokenProcessor) {
		jerseyConfig.register(new TokenResource(tokenProcessor));
	}


	@JsonIgnore
	@ValidationMethod(message = "Realm was emtpy")
	public boolean isRealmFilled() {
		return realm != null && !realm.isBlank();
	}

	@JsonIgnore
	@ValidationMethod(message = "Resource was emtpy")
	public boolean isResourceFilled() {
		return resource != null && !resource.isBlank();
	}

	@JsonIgnore
	@ValidationMethod(message = "Secret not found")
	public boolean isSecretFilled() {		
		if(credentials == null) {
			return false;
		}
		
		Object secret = credentials.get(CONFIDENTIAL_CREDENTIAL);
		if(secret == null) {
			return false;
		}
		
		if(!(secret instanceof String)) {
			return false;
		}
		
		return !((String)secret).isBlank();
	}
}
