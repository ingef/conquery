package com.bakdata.conquery.models.auth.oidc.passwordflow;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.xodus.MetaStorage;
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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
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
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.util.InstantiationException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.representation.ServerConfiguration;

/**
 * Realm that supports the Open ID Connect Resource-Owner-Password-Credential-Flow with a Keycloak IdP.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class OIDCResourceOwnerPasswordCredentialRealm extends ConqueryAuthenticationRealm implements AuthApiUnprotectedResourceProvider, AuthAdminUnprotectedResourceProvider, UsernamePasswordChecker {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = JwtToken.class;
	
	private final MetaStorage storage;
	private final OIDCResourceOwnerPasswordCredentialRealmFactory config;
	
	private ClientAuthentication clientAuthentication;
	
	private Optional<ServerConfiguration> serverConf;
	
	/**
	 * We only hold validated Tokens for some minutes to recheck them regulary with Keycloak.
	 */
	private LoadingCache<JwtToken, TokenIntrospectionSuccessResponse> tokenCache = CacheBuilder.newBuilder()
		.expireAfterWrite(10, TimeUnit.MINUTES)
		.build(new TokenValidator());
	
	@Override
	protected void onInit() {
		super.onInit();
		this.setCredentialsMatcher(new SkippingCredentialsMatcher());
		this.setAuthenticationTokenClass(TOKEN_CLASS);
		this.clientAuthentication = new ClientSecretBasic(new ClientID(config.getResource()), new Secret((String)config.getCredentials().get("secret")));
		
		serverConf = Optional.of(getAuthServerConf(config));
	}
	
	private static ServerConfiguration getAuthServerConf(OIDCResourceOwnerPasswordCredentialRealmFactory config) {
		AuthzClient authzClient;
		try {
			// This tries to contact the identity providers discovery endpoint and can possibly timeout
			authzClient = AuthzClient.create(config);
		} catch (Exception e) {
			log.warn("Unable to estatblish connection to auth server.", log.isTraceEnabled()? e : null );
			return null;
		}
		return authzClient.getServerConfiguration();
	}
	
	private synchronized Optional<ServerConfiguration> tryGetAuthServerConf() {
		if (serverConf.isPresent()) {
			return serverConf;
		}
		serverConf = Optional.of(getAuthServerConf(config));
		return serverConf;
	}
	
	@Override
	@SneakyThrows
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		
		TokenIntrospectionSuccessResponse successResponse = tokenCache.get((JwtToken) token);
		
//		TODO check why tokens are invalidated too early
//		if(isExpired(successResponse)){
//			tokenCache.invalidate(token);
//			throw new ExpiredCredentialsException();
//		}

		String username = successResponse.getUsername();
		if(StringUtils.isBlank(username)) {
			username = successResponse.getStringParameter("preferred_username");
		}
		if(StringUtils.isBlank(username)) {
			throw new IllegalStateException("Unable to retrieve a user identifier from validated token. Dismissing the token.");
		}
		
		UserId userId = new UserId(username);
		User user = storage.getUser(userId);
		// try to construct a new User if none could be found in the storage
		if (user == null) {
			String userLabel = successResponse.getStringParameter("name");
			user = new User(username, userLabel != null ?  userLabel : username);
			storage.addUser(user);
			log.info("Created new user: {}", user);
		}

		return new ConqueryAuthenticationInfo(user.getId(), token, this, true);
	}
	
	private boolean isExpired(TokenIntrospectionSuccessResponse tokenInstrospection) {
		LocalDateTime expTime = tokenInstrospection.getExpirationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime now = LocalDateTime.now();
		
		boolean result = expTime.isBefore(now);
		if(result) {
			log.debug("Provided token expired at {} ( now is {})", expTime, now);
		}
		return result;
	}

	/**
	 * Is called by the CacheLoader, so the Token is not validated on every request.
	 */
	private TokenIntrospectionSuccessResponse validateToken(AuthenticationToken token) throws ParseException, IOException {
		Optional<ServerConfiguration> servConf = tryGetAuthServerConf();
		if(servConf.isEmpty()) {
			throw new InstantiationException("Unable to gain server configuration");
		}
		
		TokenIntrospectionRequest request = new TokenIntrospectionRequest(URI.create(servConf.get().getIntrospectionEndpoint()) , clientAuthentication, new TypelessAccessToken((String) token.getCredentials()));
		
		TokenIntrospectionResponse response = TokenIntrospectionResponse.parse(request.toHTTPRequest().send());
		
		if (!response.indicatesSuccess()) {
			log.error(response.toErrorResponse().getErrorObject().toString());
			throw new AuthenticationException("Unable to retrieve access token from auth server.");
		}
		else if (!(response instanceof TokenIntrospectionSuccessResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new AuthenticationException("Unknown token response. See log.");
		}

		TokenIntrospectionSuccessResponse successResponse = response.toSuccessResponse();
		if(!successResponse.isActive()) {
			throw new ExpiredCredentialsException();
		}
		return successResponse;
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
		Optional<ServerConfiguration> servConf = tryGetAuthServerConf();
		if(servConf.isEmpty()) {
			throw new InstantiationException("Unable to gain server configuration");
		}
		
		Secret passwordSecret = new Secret(new String(password));

		AuthorizationGrant  grant = new ResourceOwnerPasswordCredentialsGrant(username, passwordSecret);
		
		URI tokenEndpoint =  UriBuilder.fromUri(servConf.get().getTokenEndpoint()).build();

		TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, clientAuthentication, grant, Scope.parse("openid"));
		
		
		TokenResponse response = TokenResponse.parse(tokenRequest.toHTTPRequest().send());

		if (!response.indicatesSuccess()) {
			log.error( response.toErrorResponse().getErrorObject().toString());
			throw new IllegalStateException("Unable to retrieve access token from auth server.");
		}
		else if (!(response instanceof AccessTokenResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new IllegalStateException("Unknown token response. See log.");
		}

		AccessTokenResponse successResponse = (AccessTokenResponse) response;

		// Get the access token, the server may also return a refresh token
		AccessToken accessToken = successResponse.getTokens().getAccessToken();
		//RefreshToken refreshToken = successResponse.getTokens().getRefreshToken();
		return accessToken.getValue();
	}
	
	private class TokenValidator extends CacheLoader<JwtToken, TokenIntrospectionSuccessResponse>{

		@Override
		public TokenIntrospectionSuccessResponse load(JwtToken key) throws Exception {
			return validateToken(key);
		}
		
	}

}
