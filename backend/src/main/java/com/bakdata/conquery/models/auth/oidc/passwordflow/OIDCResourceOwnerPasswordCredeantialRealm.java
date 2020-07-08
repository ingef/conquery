package com.bakdata.conquery.models.auth.oidc.passwordflow;

import java.net.URI;
import java.net.URL;

import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.TokenHandler;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.basic.UsernamePasswordChecker;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.unprotected.AuthServlet.AuthAdminUnprotectedResourceProvider;
import com.bakdata.conquery.resources.unprotected.AuthServlet.AuthApiUnprotectedResourceProvider;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionErrorResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionRequest;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionSuccessResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.TypelessAccessToken;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.keycloak.authorization.client.AuthzClient;

@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class OIDCResourceOwnerPasswordCredeantialRealm extends ConqueryAuthenticationRealm implements AuthApiUnprotectedResourceProvider, AuthAdminUnprotectedResourceProvider, UsernamePasswordChecker {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = JwtToken.class;
	
	private final MasterMetaStorage storage;
	private final OIDCResourceOwnerPasswordCredeantialRealmFactory config;
	
	private ClientAuthentication clientAuthentication;
	private AuthzClient authzClient;
	
	@Override
	protected void onInit() {
		super.onInit();
		this.setCredentialsMatcher(new SkippingCredentialsMatcher());
		this.setAuthenticationTokenClass(TOKEN_CLASS);
		this.clientAuthentication = new ClientSecretBasic(new ClientID(config.getResource()), new Secret((String)config.getCredentials().get("secret")));
		
		authzClient = AuthzClient.create(config);
	}
	
	@Override
	@SneakyThrows
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		token.getCredentials();
		
		URI tokenIntrospectEndpoint =  new URL(new URL(config.getAuthServerUrl()),"realms/EVA/protocol/openid-connect/token/introspect").toURI();
		TokenIntrospectionRequest request = new TokenIntrospectionRequest(tokenIntrospectEndpoint , clientAuthentication, new TypelessAccessToken((String) token.getCredentials()));
		
		TokenIntrospectionResponse response = TokenIntrospectionResponse.parse(request.toHTTPRequest().send());
		
		if (response instanceof TokenIntrospectionErrorResponse) {
			log.error(((TokenIntrospectionErrorResponse) response).getErrorObject().toString());
			throw new IllegalStateException("Unable to retrieve access token from auth server.");
		}
		else if (!(response instanceof TokenIntrospectionSuccessResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new IllegalStateException("Unknown token response. See log.");
		}

		TokenIntrospectionSuccessResponse successResponse = (TokenIntrospectionSuccessResponse) response;

		String username = successResponse.getUsername();
		if(StringUtils.isBlank(username)) {
			username = successResponse.getStringParameter("preferred_username");
		}
		if(StringUtils.isBlank(username)) {
			throw new IllegalStateException("Unable to retrieve a user identifier from validated token. Dismissing the token.");
		}
// TODO use keycloak lib
//		AuthorizationResponse authzResponse = authzClient.authorization((String)token.getCredentials()).authorize();
		
		UserId userId = new UserId(username);
		User user = storage.getUser(userId);
		// try to construct a new User if none could be found in the storage
		if (user == null) {
			String userLabel = successResponse.getStringParameter("name");
			user = new User(username, userLabel != null ?  userLabel : username);
			storage.addUser(user);
			log.info(
				"Created new user: {}",
				user);
		}

		return new ConqueryAuthenticationInfo(user.getId(), token, this);
	}

	@Override
	public AuthenticationToken extractToken(ContainerRequestContext request) {
		return TokenHandler.extractToken(request);
	}
	
	@Override
	public void registerAdminUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig) {
		jerseyConfig.register(new TokenResource(this));
		jerseyConfig.register(LoginResource.class);
	}

	@Override
	public void registerApiUnprotectedAuthenticationResources(DropwizardResourceConfig jerseyConfig) {
		jerseyConfig.register(new TokenResource(this));
	}

	@Override
	@SneakyThrows
	public String checkCredentialsAndCreateJWT(String username, char[] password) {
		Secret passwordSecret = new Secret(new String(password));

		AuthorizationGrant  grant = new ResourceOwnerPasswordCredentialsGrant(username, passwordSecret);
				
		URI tokenEndpoint =  new URL(new URL(config.getAuthServerUrl()),"realms/EVA/protocol/openid-connect/token").toURI();

		TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, grant, Scope.parse("openid"));
		
		
		TokenResponse response = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

		if (response instanceof TokenErrorResponse) {
			log.error(((TokenErrorResponse) response).getErrorObject().toString());
			throw new IllegalStateException("Unable to retrieve access token from auth server.");
		}
		else if (!(response instanceof AccessTokenResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new IllegalStateException("Unknown token response. See log.");
		}

		AccessTokenResponse successResponse = (AccessTokenResponse) response;

		// Get the access token, the server may also return a refresh token
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		RefreshToken refreshToken = successResponse.getTokens().getRefreshToken();
		return accessToken.getValue();
	}

}
