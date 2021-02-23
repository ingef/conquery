package com.bakdata.conquery.models.auth.oidc;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenIntrospectionRequest;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionSuccessResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.TypelessAccessToken;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.keycloak.authorization.client.Configuration;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Realm that validates OpenID access tokens by delegating them to an IDP TokenIntrospection endpoint
 */
@Slf4j
@Getter
@Setter
@RequiredArgsConstructor
public class IntrospectionDelegatingRealm extends ConqueryAuthenticationRealm {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;
	private static final String GROUPS_CLAIM = "groups";

	private final MetaStorage storage;
	private final IntrospectionDelegatingRealmFactory authProviderConf;

	private ClientAuthentication clientAuthentication;


	/**
	 * We only hold validated Tokens for some minutes to recheck them regulary with Keycloak.
	 */
	private LoadingCache<BearerToken, TokenIntrospectionSuccessResponse> tokenCache = CacheBuilder.newBuilder()
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
		if (!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			log.trace("Incompatible token. Expected {}, got {}", TOKEN_CLASS, token.getClass());
			return null;
		}
		log.trace("Token has expected format!");

		TokenIntrospectionSuccessResponse successResponse = tokenCache.get((BearerToken) token);

		log.trace("Got an successful token introspection response.");

		UserId userId = extractId(successResponse);

		User user = storage.getUser(userId);

		if (user == null) {
			throw new IllegalStateException("Unable to retrieve user with id: " + userId);
		}

		return new ConqueryAuthenticationInfo(user.getId(), token, this, true);
	}

	private static UserId extractId(TokenIntrospectionSuccessResponse successResponse) {
		String identifier = successResponse.getUsername();
		if (StringUtils.isBlank(identifier)) {
			identifier = successResponse.getStringParameter("preferred_username");
		}
		if (StringUtils.isBlank(identifier)) {
			identifier = successResponse.getStringParameter("email");
		}
		if (StringUtils.isBlank(identifier)) {
			throw new IllegalStateException("Unable to retrieve a user identifier from validated token. Dismissing the token.");
		}
		UserId userId = new UserId(identifier);
		log.trace("Extracted UserId {}", userId);
		return userId;
	}

	private static String extractDisplayName(TokenIntrospectionSuccessResponse successResponse) {
		String username = successResponse.getUsername();
		if (StringUtils.isBlank(username)) {
			username = successResponse.getStringParameter("name");
		}
		if (StringUtils.isBlank(username)) {
			throw new IllegalStateException("Unable to retrieve a user identifier from validated token. Dismissing the token.");
		}

		return username;
	}

	/**
	 * Is called by the CacheLoader, so the Token is not validated on every request.
	 */
	private TokenIntrospectionSuccessResponse validateToken(AuthenticationToken token) throws ParseException, IOException {
		TokenIntrospectionRequest request = new TokenIntrospectionRequest(URI.create(authProviderConf.getIntrospectionEndpoint()), authProviderConf.getClientAuthentication(), new TypelessAccessToken((String) token.getCredentials()));

		TokenIntrospectionResponse response = TokenIntrospectionResponse.parse(request.toHTTPRequest().send());
		log.trace("Retrieved token introspection response.");
		if (!response.indicatesSuccess()) {
			HTTPResponse httpResponse = response.toHTTPResponse();
			log.error("Received the following error from the auth server while validating a token: {} {} {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getContent());
			throw new AuthenticationException("Unable to retrieve access token from auth server.");
		} else if (!(response instanceof TokenIntrospectionSuccessResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new AuthenticationException("Unknown token response. See log.");
		}

		TokenIntrospectionSuccessResponse successResponse = response.toSuccessResponse();
		log.trace("Token introspection: {}", successResponse);
		if (!successResponse.isActive()) {
			log.trace("Token was not active");
			throw new ExpiredCredentialsException();
		}
		return successResponse;
	}


	private class TokenValidator extends CacheLoader<BearerToken, TokenIntrospectionSuccessResponse> {

		@Override
		public TokenIntrospectionSuccessResponse load(BearerToken key) throws Exception {
			log.trace("Attempting to validate token");
			TokenIntrospectionSuccessResponse response = validateToken(key);

			User user = getOrCreateUser(response, extractDisplayName(response), extractId(response));
			Set<Group> mappedGroupsToDo = getMappedGroups(response);

			synchGroupMappings(user, mappedGroupsToDo);

			return response;
		}


		private void synchGroupMappings(User user, Set<Group> mappedGroupsToDo) {
			for(Group group : storage.getAllGroups()) {
				if(group.containsMember(user.getId())) {
					if(mappedGroupsToDo.contains(group)) {
						// Mapping is still valid, remove from ToDo-List
						mappedGroupsToDo.remove(group);
					} else {
						// Mapping is not valid any more remove user from group
						group.removeMember(storage, user);
					}
				}
			}

			for (Group group : mappedGroupsToDo) {
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
			user = new User(username, userLabel != null ? userLabel : username);
			storage.addUser(user);
			log.info("Created new user: {}", user);
			return user;
		}

		private Set<Group> getMappedGroups(TokenIntrospectionSuccessResponse successResponse) {
			List<String> groupNames = Objects.requireNonNullElse(successResponse.getStringListParameter(GROUPS_CLAIM), List.of());
			Stream<Pair<String, GroupId>> derivedGroupIds = groupNames.stream().map(name -> Pair.of(name, new GroupId(name)));
			Set<Group> groups = derivedGroupIds.map(this::getOrCreateGroup).collect(Collectors.toCollection(Sets::newHashSet));
			return groups;
		}

		private synchronized Group getOrCreateGroup(Pair<String, GroupId> groupNameId) {
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
