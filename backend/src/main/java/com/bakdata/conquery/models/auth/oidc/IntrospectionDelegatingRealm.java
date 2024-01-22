package com.bakdata.conquery.models.auth.oidc;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationInfo;
import com.bakdata.conquery.models.auth.ConqueryAuthenticationRealm;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.oidc.keycloak.GroupUtil;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakApi;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakGroup;
import com.bakdata.conquery.models.auth.util.SkippingCredentialsMatcher;
import com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenIntrospectionRequest;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.TokenIntrospectionSuccessResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.Audience;
import com.nimbusds.oauth2.sdk.token.TypelessAccessToken;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.BearerToken;
import org.apache.shiro.authc.ExpiredCredentialsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.pam.UnsupportedTokenException;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.jetbrains.annotations.Nullable;

/**
 * Realm that validates OpenID access tokens by delegating them to an oauth tokenIntrospection endpoint.
 * <p>
 * If {@link IntrospectionDelegatingRealmFactory#getGroupIdAttribute()} is defined, it also maps groups in keycloak, which have this attribute set
 * to the corresponding group attribute in conquery and synchronizes the user group membership.
 */
@Slf4j
@Getter
@Setter
public class IntrospectionDelegatingRealm extends AuthenticatingRealm implements ConqueryAuthenticationRealm {

	private static final Class<? extends AuthenticationToken> TOKEN_CLASS = BearerToken.class;

	private final IntrospectionDelegatingRealmFactory authProviderConf;
	private final MetaStorage storage;
	private final KeycloakApi keycloakApi;

	private ClientAuthentication clientAuthentication;


	/**
	 * We only hold validated Tokens for some minutes to re-setup users and reduce fan-out.
	 */
	private LoadingCache<String, JWTClaimsSet> tokenCache = CacheBuilder.newBuilder()
																		.expireAfterWrite(5, TimeUnit.MINUTES)
																		.build(new UserClaimsSetupService());

	public IntrospectionDelegatingRealm(MetaStorage storage, IntrospectionDelegatingRealmFactory authProviderConf, KeycloakApi keycloakApi) {
		this.storage = storage;
		this.authProviderConf = authProviderConf;
		this.keycloakApi = keycloakApi;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.setCredentialsMatcher(SkippingCredentialsMatcher.INSTANCE);
		this.setAuthenticationTokenClass(TOKEN_CLASS);
	}

	@Override
	@SneakyThrows
	public ConqueryAuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		if (!(TOKEN_CLASS.isAssignableFrom(token.getClass()))) {
			log.trace("Incompatible token. Expected {}, got {}", TOKEN_CLASS, token.getClass());
			return null;
		}
		log.trace("Token has expected format!");
		final BearerToken bearertoken = (BearerToken) token;

		// Validate token on every request
		validateToken(bearertoken);

		JWTClaimsSet claimsSet;

		// Try to prepare corresponding user if necessary (on new token).
		// This might fail if the tokes does not have the required audience set.
		try {
			claimsSet = tokenCache.get(bearertoken.getToken());
		}
		catch (UncheckedExecutionException e) {
			final Throwable cause = e.getCause();
			if (cause instanceof AuthenticationException) {
				throw cause;
			}
			throw e;
		}

		UserId userId = getUserId(claimsSet);

		final User user = storage.getUser(userId);

		return new ConqueryAuthenticationInfo(user, token, this, true);
	}


	/**
	 * Is called on every request to ensure that the token is still valid.
	 */
	private void validateToken(AuthenticationToken token) throws ParseException, IOException {
		// Build introspection request
		TokenIntrospectionRequest
				request =
				new TokenIntrospectionRequest(URI.create(authProviderConf.getIntrospectionEndpoint()), authProviderConf.getClientAuthentication(), new TypelessAccessToken((String) token.getCredentials()));

		// Send introspection request
		TokenIntrospectionResponse response = TokenIntrospectionResponse.parse(request.toHTTPRequest().send());

		log.trace("Retrieved token introspection response.");
		if (!response.indicatesSuccess()) {
			HTTPResponse httpResponse = response.toHTTPResponse();
			log.error("Received the following error from the auth server while validating a token: {} {} {}", httpResponse.getStatusCode(), httpResponse.getStatusMessage(), httpResponse.getContent());
			throw new AuthenticationException("Unable to retrieve access token from auth server.");
		}
		else if (!(response instanceof TokenIntrospectionSuccessResponse)) {
			log.error("Unknown token response {}.", response.getClass().getName());
			throw new AuthenticationException("Unknown token response. See log.");
		}

		TokenIntrospectionSuccessResponse successResponse = response.toSuccessResponse();
		if (log.isTraceEnabled()) {
			log.trace("Token introspection: {}", successResponse.toJSONObject());
		}
		if (!successResponse.isActive()) {
			log.trace("Token was not active");
			throw new ExpiredCredentialsException();
		}

		log.trace("Got an successful token introspection response: {}", log.isTraceEnabled() ? successResponse.toJSONObject().toString() : "");

	}


	private void validateAudiences(JWTClaimsSet claims) {
		// Check if the token is intended for our client/resource
		final Audience expectedAudience = new Audience(authProviderConf.getResource());
		final List<Audience> providedAudiences = Audience.create(claims.getAudience());

		if (providedAudiences == null) {
			throw new IncorrectCredentialsException("Token does not contain audiences.");
		}

		if (!providedAudiences.contains(expectedAudience)) {
			throw new IncorrectCredentialsException("Audience does not match. Expected: '"
													+ expectedAudience.getValue()
													+ "' (was: '"
													+ claims.getAudience()
													+ "')");
		}
	}

	private static UserId getUserId(JWTClaimsSet claims) {
		final String subject = claims.getSubject();
		UserId userId = new UserId(subject);
		log.trace("Extracted UserId {}", userId);
		return userId;
	}

	private static String extractDisplayName(JWTClaimsSet claims) {
		try {
			final String name = claims.getStringClaim("name");

			if (StringUtils.isBlank(name)) {
				throw new UnsupportedTokenException("Claim 'name' was empty");
			}
			return name;
		}
		catch (java.text.ParseException e) {
			throw new IncorrectCredentialsException("Unable to extract username from token", e);
		}
	}


	/**
	 * Validates token and synchronizes user and its group memberships with keycloak.
	 */
	private class UserClaimsSetupService extends CacheLoader<String, JWTClaimsSet> {

		@Override
		public JWTClaimsSet load(String token) throws Exception {

			final SignedJWT jwt = SignedJWT.parse(token);

			final JWTClaimsSet claimsSet = jwt.getJWTClaimsSet();


			// Check if token was intended for us
			validateAudiences(claimsSet);

			final User user = getOrCreateUser(claimsSet);

			// Map groups if group-id attribute is set
			final String groupIdAttribute = authProviderConf.getGroupIdAttribute();
			if (Strings.isNotBlank(groupIdAttribute)) {
				final Set<Group> memberships = getUserGroups(claimsSet, groupIdAttribute);
				syncGroupMappings(user, memberships);
			}

			return claimsSet;
		}

		@Nullable
		private Set<Group> getUserGroups(JWTClaimsSet claims, String groupIdAttribute) {
			final Set<KeycloakGroup> userGroups = keycloakApi.getUserGroups(claims.getSubject());
			final Set<KeycloakGroup> groupHierarchy = keycloakApi.getGroupHierarchy();

			// Collect direct and indirect group memberships
			Set<KeycloakGroup> allMemberships = GroupUtil.getAllUserGroups(userGroups, groupHierarchy);

			// Extract eva-group-id from attributes
			return allMemberships.stream()
								 .map(this::tryGetGroup)
								 .flatMap(Optional::stream)
								 .collect(Collectors.toSet());
		}


		private Optional<Group> tryGetGroup(KeycloakGroup keycloakGroup) {

			final Map<String, String> attributes = keycloakGroup.attributes();
			if (attributes == null) {
				log.trace("Not mapping keycloak group because it has no attributes set: {}", keycloakGroup);
				return Optional.empty();
			}

			// Extract group id
			final String groupIdString = attributes.get(authProviderConf.getGroupIdAttribute());
			if (groupIdString == null) {
				log.trace("Not mapping keycloak group because it has no attribute '{}': {}", authProviderConf.getGroupIdAttribute(), keycloakGroup);
				return Optional.empty();
			}
			if (Strings.isBlank(groupIdString)) {
				log.error("Cannot map keycloak group, because group id attribute was blank: {}", keycloakGroup);
				return Optional.empty();
			}

			// Parse group id
			GroupId groupId;
			try {
				groupId = GroupId.Parser.INSTANCE.parse(groupIdString);
			}
			catch (Exception e) {
				log.error("Cannot parse '{}' as a GroupId. Skipping", groupIdString);
				return Optional.empty();
			}

			final Group group = storage.getGroup(groupId);

			if (group != null) {
				// Found existing group
				return Optional.of(group);
			}

			// Create a new group
			return Optional.of(createGroup(groupId.getGroup(), keycloakGroup.name()));
		}

		private synchronized Group createGroup(String name, String label) {
			// TODO mark group as managed by keycloak
			final Group group = new Group(name, label, storage);

			// Recheck group existence in synchronized part
			final Group existing = storage.getGroup(group.getId());

			if (existing != null) {
				// Found existing group
				log.debug("Skip group creation, because group '{}' existed", group.getId());
				return existing;
			}

			log.info("Creating new Group: {}", group);
			group.updateStorage();
			return group;
		}


		private void syncGroupMappings(User user, Set<Group> mappedGroupsToDo) {
			// TODO mark mappings as managed by keycloak
			for (Group group : storage.getAllGroups()) {
				if (group.containsMember(user)) {
					if (mappedGroupsToDo.contains(group)) {
						// Mapping is still valid, remove from todo-list
						mappedGroupsToDo.remove(group);
					}
					else {
						// Mapping is not valid any more remove user from group
						group.removeMember(user);
					}
				}
			}

			for (Group group : mappedGroupsToDo) {
				group.addMember(user);
			}
		}


		private synchronized User getOrCreateUser(JWTClaimsSet claims) {
			UserId userId = getUserId(claims);
			final String displayName = extractDisplayName(claims);

			User user = storage.getUser(userId);

			if (user != null) {
				log.trace("Found existing user: {}", user);
				// Update display name if necessary
				if (!user.getLabel().equals(displayName)) {
					log.info("Updating display name of user [{}]: '{}' -> '{}'", user.getName(), user.getLabel(), displayName);
					user.setLabel(displayName);
					user.updateStorage();
				}

				return user;
			}

			// Construct a new User if none could be found in the storage
			user = new User(userId.getName(), displayName, storage);
			storage.addUser(user);
			log.info("Created new user: {}", user);

			return user;
		}

	}

}
