package com.bakdata.conquery.models.auth.oidc.passwordflow;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.TokenHandler;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.basic.UsernamePasswordChecker;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.unprotected.AuthServlet.AuthAdminUnprotectedResourceProvider;
import com.bakdata.conquery.resources.unprotected.AuthServlet.AuthApiUnprotectedResourceProvider;
import com.bakdata.conquery.resources.unprotected.LoginResource;
import com.bakdata.conquery.resources.unprotected.TokenResource;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
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
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.TypelessAccessToken;
import io.dropwizard.jersey.DropwizardResourceConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExpiredCredentialsException;

/**
 * Realm that supports the Open ID Connect Resource-Owner-Password-Credential-Flow with a Keycloak IdP.
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class OIDCResourceOwnerPasswordCredentialRealm<C extends OIDCAuthenticationConfig> extends ConqueryAuthenticationRealm implements AuthApiUnprotectedResourceProvider, AuthAdminUnprotectedResourceProvider, UsernamePasswordChecker {

	public static final String CONFIDENTIAL_CREDENTIAL = "secret";
	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = JwtToken.class;
	private static final String GROUPS_CLAIM = "groups";
	
	private final MetaStorage storage;
	private final OIDCAuthenticationConfig authProviderConf;
	
	private ClientAuthentication clientAuthentication;
	
	
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
	}
	
	@Override
	@SneakyThrows
	protected ConqueryAuthenticationInfo doGetConqueryAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		
		TokenIntrospectionSuccessResponse successResponse = tokenCache.get((JwtToken) token);
		
		UserId userId = extractId(successResponse);

		User user = storage.getUser(userId);
		
		if(user == null) {
			throw new IllegalStateException("Unable to retrieve user with id: " + userId);
		}

		return new ConqueryAuthenticationInfo(user.getId(), token, this, true);
	}

	private UserId extractId(TokenIntrospectionSuccessResponse successResponse) {
		String identifier = successResponse.getUsername();
		if(StringUtils.isBlank(identifier)) {
			identifier = successResponse.getStringParameter("preferred_username");
		}
		if(StringUtils.isBlank(identifier)) {
			identifier = successResponse.getStringParameter("email");
		}
		if(StringUtils.isBlank(identifier)) {
			throw new IllegalStateException("Unable to retrieve a user identifier from validated token. Dismissing the token.");
		}
		
		return new UserId(identifier);
	}
	
	private String extractDisplayName(TokenIntrospectionSuccessResponse successResponse) {
		String username = successResponse.getUsername();
		if(StringUtils.isBlank(username)) {
			username = successResponse.getStringParameter("name");
		}
		if(StringUtils.isBlank(username)) {
			throw new IllegalStateException("Unable to retrieve a user identifier from validated token. Dismissing the token.");
		}
		
		return username;
	}

	/**
	 * Is called by the CacheLoader, so the Token is not validated on every request.
	 */
	private TokenIntrospectionSuccessResponse validateToken(AuthenticationToken token) throws ParseException, IOException {
		TokenIntrospectionRequest request = new TokenIntrospectionRequest(URI.create(authProviderConf.getIntrospectionEndpoint()) , authProviderConf.getClientAuthentication(), new TypelessAccessToken((String) token.getCredentials()));
		
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
		
		Secret passwordSecret = new Secret(new String(password));

		AuthorizationGrant  grant = new ResourceOwnerPasswordCredentialsGrant(username, passwordSecret);
		
		URI tokenEndpoint =  UriBuilder.fromUri(authProviderConf.getTokenEndpoint()).build();

		TokenRequest tokenRequest = new TokenRequest(tokenEndpoint, authProviderConf.getClientAuthentication(), grant, Scope.parse("openid"));
		
		
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
			TokenIntrospectionSuccessResponse response = validateToken(key);
			
			User user = getOrCreateUser(response, extractDisplayName(response), extractId(response));
			Set<Group> mappedGroupsToDo = getMappedGroups(response);

			synchGroupMappings(user, mappedGroupsToDo);
			
			return response;
		}


		private void synchGroupMappings(User user, Set<Group> mappedGroupsToDo) {
			for(Group group : storage.getAllGroups()) {
				if(group.containsMember(user)) {
					if(mappedGroupsToDo.contains(group)) {
						// Mapping is still valid, remove from ToDo-List
						mappedGroupsToDo.remove(group);
					}
					else {
						// Mapping is not valid any more remove user from group
						group.removeMember(storage, user);
					}
				}
			}
			
			for(Group group : mappedGroupsToDo) {
				group.addMember(storage, user);
			}
		}


		private synchronized User getOrCreateUser(TokenIntrospectionSuccessResponse successResponse, String username, UserId userId) {
			User user = storage.getUser(userId);
			if (user != null) {
				return user;
			}
			// try to construct a new User if none could be found in the storage
			String userLabel = successResponse.getStringParameter("name");
			user = new User(username, userLabel != null ?  userLabel : username);
			storage.addUser(user);
			log.info("Created new user: {}", user);
			return user;
		}
		
		private Set<Group> getMappedGroups(TokenIntrospectionSuccessResponse successResponse) {
			List<String> groupNames = Objects.requireNonNullElse(successResponse.getStringListParameter(GROUPS_CLAIM), List.of());
			Stream<Pair<String,GroupId>> derivedGroupIds = groupNames.stream().map(name -> Pair.of(name,new GroupId(name)));
			Set<Group> groups =  derivedGroupIds.map(this::getOrCreateGroup).collect(Collectors.toCollection(Sets::newHashSet));
			return groups;
		}
		
		private synchronized Group getOrCreateGroup(Pair<String,GroupId> groupNameId) {
			Group group = storage.getGroup(groupNameId.getValue());
			if (group != null) {
				return group;
			}
			group = new Group(groupNameId.getValue().getGroup(), groupNameId.getKey());
			storage.addGroup(group);
			log.info("Created new group: {}", group);
			return group;
		}
		
	}

}
