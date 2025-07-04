package com.bakdata.conquery.models.auth;

import static com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory.CONFIDENTIAL_CREDENTIAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.Validator;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealm;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakApi;
import com.bakdata.conquery.models.auth.oidc.keycloak.KeycloakGroup;
import com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.extensions.GroupExtension;
import com.bakdata.conquery.util.extensions.MetaStorageExtension;
import com.bakdata.conquery.util.extensions.MockServerExtension;
import com.bakdata.conquery.util.extensions.UserExtension;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.validation.BaseValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.BearerToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;

@Slf4j
public class IntrospectionDelegatingRealmTest {
	@RegisterExtension
	private static final MockServerExtension OIDC_SERVER = new MockServerExtension(ClientAndServer.startClientAndServer(), IntrospectionDelegatingRealmTest::initOIDCServer);

	@RegisterExtension
	@Order(0)
	private static final MetaStorageExtension STORAGE_EXTENTION = new MetaStorageExtension(new MetricRegistry());
	private static final MetaStorage STORAGE = STORAGE_EXTENTION.getMetaStorage();
	private static final IntrospectionDelegatingRealmFactory CONFIG = new IntrospectionDelegatingRealmFactory();
	private static final Validator VALIDATOR = BaseValidator.newValidator();

	private static final String GROUP_ID_ATTRIBUTE = "group-id";

	// User 1
	private static final String USER_1_NAME = "test_name1";
	@RegisterExtension
	private static final UserExtension USER_1_EXTENSION = new UserExtension(STORAGE, USER_1_NAME);
	private static final String USER_1_PASSWORD = "test_password1";
	public static final String BACKEND_AUD = "backend";
	public static final String SOME_SECRET = "secret";
	private static final String USER_1_TOKEN = JWT.create()
												  .withSubject(USER_1_NAME)
												  .withAudience(BACKEND_AUD)
												  .withClaim("name", USER_1_NAME)
												  .sign(Algorithm.HMAC256(SOME_SECRET));
	private static final BearerToken USER1_TOKEN_WRAPPED = new BearerToken(USER_1_TOKEN);

	// User 2
	private static final String USER_2_NAME = "test_name2";
	private static final String USER_2_LABEL = "test_label2";
	@RegisterExtension
	private static final UserExtension USER_2_EXTENSION = new UserExtension(STORAGE, USER_2_NAME, USER_2_LABEL);
	private static final String USER_2_TOKEN = JWT.create()
												  .withSubject(USER_2_NAME)
												  .withAudience(BACKEND_AUD)
												  .withClaim("name", USER_2_LABEL)
												  .sign(Algorithm.HMAC256(SOME_SECRET));
	private static final BearerToken USER_2_TOKEN_WRAPPED = new BearerToken(USER_2_TOKEN);

	// User 3 existing
	private static final String USER_3_NAME = "test_name3";
	private static final String USER_3_LABEL = "test_label3";
	@RegisterExtension
	private static final UserExtension USER_3_EXTENSION = new UserExtension(STORAGE, USER_3_NAME, USER_3_LABEL);
	private static final String USER_3_TOKEN = JWT.create()
												  .withSubject(USER_3_NAME)
												  .withAudience(BACKEND_AUD)
												  .withClaim("name", USER_3_LABEL)
												  .sign(Algorithm.HMAC256(SOME_SECRET));
	private static final BearerToken USER_3_TOKEN_WRAPPED = new BearerToken(USER_3_TOKEN);

	// Groups
	private static final String GROUPNAME_1 = "group1";
	@RegisterExtension
	private static final GroupExtension GROUP_1_EXISTING_EXTENSION = new GroupExtension(STORAGE, GROUPNAME_1);
	public static KeycloakGroup KEYCLOAK_GROUP_1;

	private static final String GROUPNAME_2 = "group2"; // Group is created during test
	public static KeycloakGroup KEYCLOAK_GROUP_2;
	public static final URI FRONT_CHANNEL_LOGOUT = URI.create("http://localhost:%d/realms/test_realm/protocol/openid-connect/logout".formatted(OIDC_SERVER.getPort()));

	private static TestRealm REALM;

	private static KeycloakApi KEYCLOAK_API;

	@BeforeAll
	public static void beforeAll() {
		KEYCLOAK_GROUP_1 = new KeycloakGroup(UUID.randomUUID().toString(), "Group1", "g1", Map.of(GROUP_ID_ATTRIBUTE, GROUP_1_EXISTING_EXTENSION.getGroup().getId().toString()), Set.of());
		KEYCLOAK_GROUP_2 = new KeycloakGroup(UUID.randomUUID().toString(), "Group2", "g2", Map.of(GROUP_ID_ATTRIBUTE, new GroupId(GROUPNAME_2).toString()), Set.of());

		KEYCLOAK_API = mock(KeycloakApi.class);
		doAnswer(invocation -> Set.of(KEYCLOAK_GROUP_1, KEYCLOAK_GROUP_2)).when(KEYCLOAK_API)
																		  .getGroupHierarchy();
		doAnswer(
				invocation -> {
					final String userId = invocation.getArgument(0);
					if (userId.equals(USER_2_NAME)) {
						return Set.of(KEYCLOAK_GROUP_1, KEYCLOAK_GROUP_2);
					}
					return Set.of();
				}
		).when(KEYCLOAK_API).getUserGroups(any(String.class));

		initRealm();

	}

	@BeforeEach
	public void beforeEach() {
		// clear storage underlying data structures
		STORAGE.clear();

		// Clear Token Cache
		REALM.getTokenCache().invalidateAll();

		// add existing group to storage
		STORAGE.addGroup(GROUP_1_EXISTING_EXTENSION.getGroup());
	}


	private static void initRealm() {
		CONFIG.setRealm(OIDCMockServer.REALM_NAME);
		CONFIG.setResource("backend");
		CONFIG.setGroupIdAttribute(GROUP_ID_ATTRIBUTE);
		CONFIG.setCredentials(Map.of(CONFIDENTIAL_CREDENTIAL, "test_cred"));
		CONFIG.setAuthServerUrl(OIDC_SERVER.baseUrl());

		ValidatorHelper.failOnError(log, VALIDATOR.validate(CONFIG));

		REALM = new TestRealm(STORAGE, CONFIG);
	}

	private static void initOIDCServer(ClientAndServer mockServer) {
		// Mock username-password-for-token exchange
		OIDCMockServer.init(
				mockServer,
				(server) -> {
					server.when(
								  request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", OIDCMockServer.REALM_NAME))
										   .withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")).withBody(
												   params(
														   param("password", USER_1_PASSWORD),
														   param("grant_type", "password"),
														   param("username", USER_1_NAME),
														   param("scope", "openid")
												   )))
						  .respond(
								  response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
											.withBody("{\"token_type\" : \"Bearer\",\"access_token\" : \"" + USER_1_TOKEN + "\"}"));

					// Block other exchange requests (this has a lower prio than the above)
					server.when(
								  request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", OIDCMockServer.REALM_NAME)))
						  .respond(
								  response().withStatusCode(HttpStatus.SC_FORBIDDEN).withContentType(MediaType.APPLICATION_JSON_UTF_8)
											.withBody("{\"error\" : \"Wrong username or password\""));

					// Mock token introspection
					// For USER 1
					server.when(
								  request().withMethod("POST")
										   .withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", OIDCMockServer.REALM_NAME))
										   .withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
										   .withBody(params(param("token_type_hint", "access_token"), param("token", USER_1_TOKEN))))
						  .respond(
								  response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
											.withBody("{\"username\" : \"" + USER_1_NAME + "\", \"active\": true}"));
					// For USER 2
					server.when(
								  request().withMethod("POST")
										   .withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", OIDCMockServer.REALM_NAME))
										   .withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
										   .withBody(params(param("token_type_hint", "access_token"), param("token", USER_2_TOKEN))))
						  .respond(
								  response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
											.withBody("{\"username\" : \"" + USER_2_NAME + "\",\"name\" : \"" + USER_2_LABEL + "\", \"active\": true}"));
					// For USER 3
					server.when(
								  request().withMethod("POST")
										   .withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", OIDCMockServer.REALM_NAME))
										   .withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
										   .withBody(params(param("token_type_hint", "access_token"), param("token", USER_3_TOKEN))))
						  .respond(
								  response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
											.withBody("{\"username\" : \"" + USER_3_NAME + "\",\"name\" : \"" + USER_3_LABEL + "\", \"active\": true}"));
				}
		);
	}

	@Test
	public void tokenIntrospectionSimpleUserNew() {
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER1_TOKEN_WRAPPED);

		assertThat(info)
				.usingRecursiveComparison()
				.usingOverriddenEquals()
				.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
				.ignoringFieldsOfTypes(User.ShiroUserAdapter.class)
				.isEqualTo(new ConqueryAuthenticationInfo(USER_1_EXTENSION.getUser(), USER1_TOKEN_WRAPPED, REALM, true, FRONT_CHANNEL_LOGOUT));
		assertThat(STORAGE.getAllUsers()).containsOnly(new User(USER_1_NAME, USER_1_NAME, STORAGE_EXTENTION.getMetaStorage()));
	}

	@Test
	public void tokenIntrospectionSimpleUserExisting() {

		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER1_TOKEN_WRAPPED);

		assertThat(info)
				.usingRecursiveComparison()
				.usingOverriddenEquals()
				.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
				.isEqualTo(new ConqueryAuthenticationInfo(USER_1_EXTENSION.getUser(), USER1_TOKEN_WRAPPED, REALM, true, FRONT_CHANNEL_LOGOUT));
		assertThat(STORAGE.getAllUsers()).containsOnly(USER_1_EXTENSION.getUser());
	}

	@Test
	public void tokenIntrospectionGroupedUser() {

		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER_2_TOKEN_WRAPPED);

		final ConqueryAuthenticationInfo expected = new ConqueryAuthenticationInfo(USER_2_EXTENSION.getUser(), USER_2_TOKEN_WRAPPED, REALM, true, FRONT_CHANNEL_LOGOUT);
		assertThat(info)
				.usingRecursiveComparison()
				.usingOverriddenEquals()
				.isEqualTo(expected);
		assertThat(STORAGE.getAllUsers()).containsOnly(USER_2_EXTENSION.getUser());
		assertThat(STORAGE.getAllGroups()).hasSize(2); // Pre-existing group and a second group that has been added in the process
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).contains(new UserId(USER_2_NAME));
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_2)).getMembers()).contains(new UserId(USER_2_NAME));
	}

	@Test
	public void tokenIntrospectionGroupedUserRemoveGroupMapping() {
		GROUP_1_EXISTING_EXTENSION.getGroup().addMember(USER_3_EXTENSION.getUser().getId());

		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).contains(new UserId(USER_3_NAME));

		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER_3_TOKEN_WRAPPED);

		assertThat(info)
				.usingRecursiveComparison()
				.usingOverriddenEquals()
				.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
				.isEqualTo(new ConqueryAuthenticationInfo(USER_3_EXTENSION.getUser(), USER_3_TOKEN_WRAPPED, REALM, true, FRONT_CHANNEL_LOGOUT));
		assertThat(STORAGE.getAllUsers()).containsOnly(USER_3_EXTENSION.getUser());
		assertThat(STORAGE.getAllGroups()).hasSize(1); // Pre-existing group 
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).doesNotContain(new UserId(USER_3_NAME));
	}

	private static class TestRealm extends IntrospectionDelegatingRealm {

		public TestRealm(MetaStorage storage, IntrospectionDelegatingRealmFactory config) {
			super(storage, config, KEYCLOAK_API);
		}

	}
}
