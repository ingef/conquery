package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.validation.Validator;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.basic.TokenHandler;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealm;
import com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import io.dropwizard.validation.BaseValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

@Slf4j
public class OIDCResourceOwnerPasswordCredentialRealmTest {

	private static final MetaStorage STORAGE = mock(MetaStorage.class);
	private static final Map<UserId,User> USERS = new HashedMap<>();
	private static final Map<GroupId,Group> GROUPS = new HashedMap<>();
	private static final OIDCResourceOwnerPasswordCredentialRealmFactory CONFIG = new OIDCResourceOwnerPasswordCredentialRealmFactory();
	private static final Validator VALIDATOR = BaseValidator.newValidator();
	private static final TestRealm REALM = new TestRealm(STORAGE, CONFIG);

	private static final int MOCK_SERVER_PORT = 1080;
	private static final String MOCK_SERVER_URL = "http://localhost:" + MOCK_SERVER_PORT;
	private static final String REALM_NAME = "test_relam";
	
	// User 1
	private static final String USERNAME_1 = "test_name1";
	private static final String PASSWORD_1 = "test_password1";
	private static final String VALID_DUMMY_TOKEN = JWT.create().withClaim("name", USERNAME_1).sign(Algorithm.HMAC256("secret"));;
	private static final JwtToken VALID_DUMMY_TOKEN_WRAPPED = new TokenHandler.JwtToken(VALID_DUMMY_TOKEN);

	// User 2
	private static final String USERNAME_2 = "test_name2";
	private static final String USERLABEL_2 = "test_label2";
	private static final String PASSWORD_2 = "test_password2";
	private static final String VALID_DUMMY_TOKEN_WITH_GROUPING = JWT.create().withClaim("name", USERNAME_2).sign(Algorithm.HMAC256("secret"));;
	private static final JwtToken VALID_DUMMY_TOKEN_WITH_GROUPING_WRAPPED = new TokenHandler.JwtToken(VALID_DUMMY_TOKEN_WITH_GROUPING);
	
	// Groups
	private static final String GROUPNAME_1 = "group1";
	private static final Group GROUP_1_EXISTING = new Group(GROUPNAME_1, GROUPNAME_1);
	private static final String GROUPNAME_2 = "group2"; // Group is created during test
	
	private static ClientAndServer MOCK_SERVER;

	@BeforeAll
	public static void beforeAll() {
		// Mock user handling in storage
		doAnswer(invocation -> {
			final User user = invocation.getArgument(0);
			if(USERS.put(user.getId(), user) != null) {
				throw new IllegalStateException("There was already a mapping for the user " + user);
			}
			return null;
		}).when(STORAGE).addUser(any());
		doAnswer(invocation -> {
			final User user = invocation.getArgument(0);
			USERS.put(user.getId(), user);

			return null;
		}).when(STORAGE).updateUser(any());
		doAnswer(invocation -> {
			final UserId userId = invocation.getArgument(0);
			return USERS.get(userId);
		}).when(STORAGE).getUser(any());
		// Mock group handling
		doAnswer(invocation -> {
			final Group group = invocation.getArgument(0);
			if(GROUPS.put(group.getId(), group) != null) {
				throw new IllegalStateException("There was already a mapping for the group " + group);
			}
			return null;
		}).when(STORAGE).addGroup(any());
		doAnswer(invocation -> {
			final Group group = invocation.getArgument(0);
			GROUPS.put(group.getId(), group);

			return null;
		}).when(STORAGE).updateGroup(any());
		doAnswer(invocation -> {
			final GroupId groupId = invocation.getArgument(0);
			return GROUPS.get(groupId);
		}).when(STORAGE).getGroup(any());
		// Add a pre-existing group
		STORAGE.addGroup(GROUP_1_EXISTING);
		
		CONFIG.setRealm(REALM_NAME);
		CONFIG.setResource("test_cred");
		CONFIG.setCredentials(Map.of(OIDCResourceOwnerPasswordCredentialRealm.CONFIDENTIAL_CREDENTIAL, "test_cred"));
		CONFIG.setAuthServerUrl(MOCK_SERVER_URL);

		ValidatorHelper.failOnError(log, VALIDATOR.validate(CONFIG));

		MOCK_SERVER = startClientAndServer(MOCK_SERVER_PORT);

		// Mock well-known discovery endpoint
		MOCK_SERVER.when(request().withMethod("GET").withPath(String.format("/realms/%s/.well-known/uma2-configuration", REALM_NAME)))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withBody(
					"{\"issuer\":\""
						+ MOCK_SERVER_URL
						+ "/realms/EVA\",\"authorization_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/protocol/openid-connect/auth\",\"token_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/protocol/openid-connect/token\",\"introspection_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/protocol/openid-connect/token/introspect\",\"end_session_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/protocol/openid-connect/logout\",\"jwks_uri\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/protocol/openid-connect/certs\",\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"password\",\"client_credentials\"],\"response_types_supported\":[\"code\",\"none\",\"id_token\",\"token\",\"id_token token\",\"code id_token\",\"code token\",\"code id_token token\"],\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],\"registration_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/clients-registrations/openid-connect\",\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],\"token_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],\"scopes_supported\":[\"openid\",\"address\",\"email\",\"microprofile-jwt\",\"offline_access\",\"phone\",\"profile\",\"roles\",\"web-origins\"],\"resource_registration_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/authz/protection/resource_set\",\"permission_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/authz/protection/permission\",\"policy_endpoint\":\""
						+ MOCK_SERVER_URL
						+ "/realms/"
						+ REALM_NAME
						+ "/authz/protection/uma-policy\"}"));

		// Mock username-password-for-token exchange
		MOCK_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")).withBody(
					params(
						param("password", PASSWORD_1),
						param("grant_type", "password"),
						param("username", USERNAME_1),
						param("scope", "openid"))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"token_type\" : \"Bearer\",\"access_token\" : \"" + VALID_DUMMY_TOKEN + "\"}"));
		// Mock token introspection
		MOCK_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
				.withBody(params(param("token_type_hint", "access_token"), param("token", VALID_DUMMY_TOKEN))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"username\" : \"" + USERNAME_1 + "\", \"active\": true}"));
		MOCK_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
				.withBody(params(param("token_type_hint", "access_token"), param("token", VALID_DUMMY_TOKEN_WITH_GROUPING))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"username\" : \"" + USERNAME_2 + "\", \"active\": true, \"groups\":[\"" + GROUPNAME_1 + "\",\"" + GROUPNAME_2 + "\"]}"));
		// Init trap for debugging, that captures all unmapped requests
		MOCK_SERVER.when(request()).respond(new ExpectationResponseCallback() {

			@Override
			public HttpResponse handle(HttpRequest httpRequest) throws Exception {
				log.error(
					"{} on {}\n\t Headers: {}n\tBody {}",
					httpRequest.getMethod(),
					httpRequest.getPath(),
					httpRequest.getHeaderList(),
					httpRequest.getBodyAsString());
				fail("Trapped because request did not match. See log.");
				return null;
			}
		});

		REALM.init();
	}

	@Test
	public void vaildUsernamePassword() {
		String jwt = REALM.checkCredentialsAndCreateJWT(USERNAME_1, PASSWORD_1.toCharArray());
		assertThat(jwt).isEqualTo(VALID_DUMMY_TOKEN);
	}

	@Test
	public void invaildUsernamePassword() {
		// TODO make this error more descriptive
		assertThatThrownBy(() -> REALM.checkCredentialsAndCreateJWT(USERNAME_1, "bad_password".toCharArray()))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void tokenExtraction() {
		JWT.create().withClaim("name", USERNAME_1).sign(Algorithm.HMAC256("secret"));
		// Create a request with the very basics to run this test
		ContainerRequestContext request = new ContainerRequest(null, URI.create("http://localhost"), null, null,
			new MapPropertiesDelegate(), null);
		request.getHeaders().put(HttpHeaders.AUTHORIZATION, List.of("Bearer " + VALID_DUMMY_TOKEN));
		AuthenticationToken token = REALM.extractToken(request);
		assertThat(token).isEqualTo(VALID_DUMMY_TOKEN_WRAPPED);
	}

	@Test
	public void tokenIntrospectionSimpleUser() {
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(VALID_DUMMY_TOKEN_WRAPPED);
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(new UserId(USERNAME_1), VALID_DUMMY_TOKEN_WRAPPED, REALM, true));
		
		assertThat(USERS).contains(Map.entry(new UserId(USERNAME_1), new User(USERNAME_1, USERNAME_1)));
	}
	
	@Test
	public void tokenIntrospectionGroupedUser() {
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(VALID_DUMMY_TOKEN_WITH_GROUPING_WRAPPED);
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(new UserId(USERNAME_2), VALID_DUMMY_TOKEN_WITH_GROUPING_WRAPPED, REALM, true));
		
		assertThat(USERS).contains(Map.entry(new UserId(USERNAME_2), new User(USERNAME_2, USERNAME_1)));
		assertThat(GROUPS).hasSize(2); // Pre-existing group and a second group that has been added in the process
		assertThat(GROUPS.get(new GroupId(GROUPNAME_1)).getMembers()).contains(USERS.get(new UserId(USERNAME_2)));
		assertThat(GROUPS.get(new GroupId(GROUPNAME_2)).getMembers()).contains(USERS.get(new UserId(USERNAME_2)));
	}

	@AfterAll
	public static void afterAll() {
		MOCK_SERVER.stop();
	}

	private static class TestRealm extends OIDCResourceOwnerPasswordCredentialRealm {

		public TestRealm(MetaStorage storage, OIDCResourceOwnerPasswordCredentialRealmFactory config) {
			super(storage, config);
		}

	}
}
