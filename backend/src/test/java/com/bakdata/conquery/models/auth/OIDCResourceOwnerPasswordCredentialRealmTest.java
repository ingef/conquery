package com.bakdata.conquery.models.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.basic.TokenHandler;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealm;
import com.bakdata.conquery.models.auth.oidc.passwordflow.OIDCResourceOwnerPasswordCredentialRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import io.dropwizard.validation.BaseValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import javax.validation.Validator;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

@Slf4j
public class OIDCResourceOwnerPasswordCredentialRealmTest {

	private static final MetaStorage STORAGE = new MetaStorage(null, new NonPersistentStoreFactory(), Collections.emptyList(), null);
	private static final OIDCResourceOwnerPasswordCredentialRealmFactory CONFIG = new OIDCResourceOwnerPasswordCredentialRealmFactory();
	private static final Validator VALIDATOR = BaseValidator.newValidator();
	private static final TestRealm REALM = new TestRealm(STORAGE, CONFIG);

	private static final int MOCK_SERVER_PORT = 1080;
	private static final String MOCK_SERVER_URL = "http://localhost:" + MOCK_SERVER_PORT;
	private static final String REALM_NAME = "test_relam";
	
	// User 1
	private static final String USER_1_NAME = "test_name1";
	private static final String USER_1_PASSWORD = "test_password1";
	private static final String USER_1_TOKEN = JWT.create().withClaim("name", USER_1_NAME).sign(Algorithm.HMAC256("secret"));;
	private static final JwtToken USER1_TOKEN_WRAPPED = new TokenHandler.JwtToken(USER_1_TOKEN);

	// User 2
	private static final String USER_2_NAME = "test_name2";
	private static final String USER_2_LABEL = "test_label2";
	private static final String USER_2_TOKEN = JWT.create().withClaim("name", USER_2_NAME).sign(Algorithm.HMAC256("secret"));;
	private static final JwtToken USER_2_TOKEN_WRAPPED = new TokenHandler.JwtToken(USER_2_TOKEN);

	// User 3 existing
	private static final String USER_3_NAME = "test_name3";
	private static final String USER_3_LABEL = "test_label3";
	private static final String USER_3_TOKEN = JWT.create().withClaim("name", USER_3_NAME).sign(Algorithm.HMAC256("secret"));;
	private static final JwtToken USER_3_TOKEN_WRAPPED = new TokenHandler.JwtToken(USER_3_TOKEN);
	// Groups
	private static final String GROUPNAME_1 = "group1";
	private static final Group GROUP_1_EXISTING = new Group(GROUPNAME_1, GROUPNAME_1);
	private static final String GROUPNAME_2 = "group2"; // Group is created during test
	
	private static ClientAndServer OIDC_SERVER;

	@BeforeAll
	public static void beforeAll() {
//		prepareStorage();
		
		initRealmConfig();

		initOIDCServer();

		REALM.init();
	}
	
	@BeforeEach
	public void beforeEach() {
		// clear storage underlying data structures
		STORAGE.clear();
		
		// Clear Token Cache
		REALM.getTokenCache().invalidateAll();
		
		// add existing group to storage 
		STORAGE.addGroup(GROUP_1_EXISTING);
	}


	private static void initRealmConfig() {
		CONFIG.setRealm(REALM_NAME);
		CONFIG.setResource("test_cred");
		CONFIG.setCredentials(Map.of(OIDCResourceOwnerPasswordCredentialRealm.CONFIDENTIAL_CREDENTIAL, "test_cred"));
		CONFIG.setAuthServerUrl(MOCK_SERVER_URL);

		ValidatorHelper.failOnError(log, VALIDATOR.validate(CONFIG));
	}

	private static void initOIDCServer() {
		OIDC_SERVER = startClientAndServer(MOCK_SERVER_PORT);

		// Mock well-known discovery endpoint (this is actually the output of keycloak)
		OIDC_SERVER.when(request().withMethod("GET").withPath(String.format("/realms/%s/.well-known/uma2-configuration", REALM_NAME)))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8).withBody(
					"{\"issuer\":\"" + MOCK_SERVER_URL + "/realms/EVA\","
						+ "\"authorization_endpoint\":\""	+ MOCK_SERVER_URL + "/realms/" + REALM_NAME	+ "/protocol/openid-connect/auth\"," 
						+ "\"token_endpoint\":\""	+ MOCK_SERVER_URL + "/realms/" + REALM_NAME	+ "/protocol/openid-connect/token\","
						+ "\"introspection_endpoint\":\""	+ MOCK_SERVER_URL + "/realms/" + REALM_NAME	+ "/protocol/openid-connect/token/introspect\""
						+ ",\"end_session_endpoint\":\""	+ MOCK_SERVER_URL + "/realms/" + REALM_NAME + "/protocol/openid-connect/logout\","
						+ "\"jwks_uri\":\"" + MOCK_SERVER_URL	+ "/realms/" + REALM_NAME + "/protocol/openid-connect/certs\","
						+ "\"grant_types_supported\":[\"authorization_code\",\"implicit\",\"refresh_token\",\"password\",\"client_credentials\"],"
						+ "\"response_types_supported\":[\"code\",\"none\",\"id_token\",\"token\",\"id_token token\",\"code id_token\",\"code token\",\"code id_token token\"],"
						+ "\"response_modes_supported\":[\"query\",\"fragment\",\"form_post\"],"
						+ "\"registration_endpoint\":\""+ MOCK_SERVER_URL + "/realms/" + REALM_NAME	+ "/clients-registrations/openid-connect\","
						+ "\"token_endpoint_auth_methods_supported\":[\"private_key_jwt\",\"client_secret_basic\",\"client_secret_post\",\"tls_client_auth\",\"client_secret_jwt\"],"
						+ "\"token_endpoint_auth_signing_alg_values_supported\":[\"PS384\",\"ES384\",\"RS384\",\"HS256\",\"HS512\",\"ES256\",\"RS256\",\"HS384\",\"ES512\",\"PS256\",\"PS512\",\"RS512\"],"
						+ "\"scopes_supported\":[\"openid\",\"address\",\"email\",\"microprofile-jwt\",\"offline_access\",\"phone\",\"profile\",\"roles\",\"web-origins\"],"
						+ "\"resource_registration_endpoint\":\"" + MOCK_SERVER_URL	+ "/realms/" + REALM_NAME + "/authz/protection/resource_set\","
						+ "\"permission_endpoint\":\"" + MOCK_SERVER_URL + "/realms/" + REALM_NAME	+ "/authz/protection/permission\","
						+ "\"policy_endpoint\":\"" + MOCK_SERVER_URL + "/realms/" + REALM_NAME + "/authz/protection/uma-policy\"}"
						));

		// Mock username-password-for-token exchange
		OIDC_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")).withBody(
					params(
						param("password", USER_1_PASSWORD),
						param("grant_type", "password"),
						param("username", USER_1_NAME),
						param("scope", "openid"))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"token_type\" : \"Bearer\",\"access_token\" : \"" + USER_1_TOKEN + "\"}"));
		// Block other exchange requests (this has a lower prio than the above)
		OIDC_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", REALM_NAME)))
			.respond(
				response().withStatusCode(HttpStatus.SC_FORBIDDEN).withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"error\" : \"Wrong username or password\""));
		
		// Mock token introspection
		// For USER 1
		OIDC_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
				.withBody(params(param("token_type_hint", "access_token"), param("token", USER_1_TOKEN))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"username\" : \"" + USER_1_NAME + "\", \"active\": true}"));
		// For USER 2
		OIDC_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
				.withBody(params(param("token_type_hint", "access_token"), param("token", USER_2_TOKEN))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"username\" : \"" + USER_2_NAME + "\",\"name\" : \"" + USER_2_LABEL + "\", \"active\": true, \"groups\":[\"" + GROUPNAME_1 + "\",\"" + GROUPNAME_2 + "\"]}"));
		// For USER 3
		OIDC_SERVER.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", REALM_NAME))
			.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
			.withBody(params(param("token_type_hint", "access_token"), param("token", USER_3_TOKEN))))
		.respond(
			response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
			.withBody("{\"username\" : \"" + USER_3_NAME + "\",\"name\" : \"" + USER_3_LABEL + "\", \"active\": true}"));
		
		// At last (so it has the lowest priority): initialize a trap for debugging, that captures all unmapped requests
		OIDC_SERVER.when(request()).respond(new ExpectationResponseCallback() {

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
	}

	@Test
	public void vaildUsernamePassword() {
		String jwt = REALM.checkCredentialsAndCreateJWT(USER_1_NAME, USER_1_PASSWORD.toCharArray());
		
		assertThat(jwt).isEqualTo(USER_1_TOKEN);
	}

	@Test
	public void invaildUsernamePassword() {
		assertThatThrownBy(
			() -> REALM.checkCredentialsAndCreateJWT(USER_1_NAME, "bad_password".toCharArray()))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	public void tokenExtraction() {
		// Create a request with the very basics to run this test
		ContainerRequestContext request = new ContainerRequest(null, URI.create("http://localhost"), null, null,
			new MapPropertiesDelegate(), null);
		request.getHeaders().put(HttpHeaders.AUTHORIZATION, List.of("Bearer " + USER_1_TOKEN));
		
		AuthenticationToken token = REALM.extractToken(request);
		
		assertThat(token).isEqualTo(USER1_TOKEN_WRAPPED);
	}

	@Test
	public void tokenIntrospectionSimpleUserNew() {
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER1_TOKEN_WRAPPED);
		
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(new UserId(USER_1_NAME), USER1_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(new User(USER_1_NAME, USER_1_NAME));
	}
	
	@Test
	public void tokenIntrospectionSimpleUserExisting() {
		User existingUser = new User(USER_1_NAME, USER_1_NAME);
		STORAGE.addUser(existingUser);
		
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER1_TOKEN_WRAPPED);
		
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(new UserId(USER_1_NAME), USER1_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(existingUser);
	}
	
	@Test
	public void tokenIntrospectionGroupedUser() {
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER_2_TOKEN_WRAPPED);
		
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(new UserId(USER_2_NAME), USER_2_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(new User(USER_2_NAME, USER_2_LABEL));
		assertThat(STORAGE.getAllGroups()).hasSize(2); // Pre-existing group and a second group that has been added in the process
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).contains(new UserId(USER_2_NAME));
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_2)).getMembers()).contains(new UserId(USER_2_NAME));
	}
	
	@Test
	public void tokenIntrospectionGroupedUserRemoveGroupMapping() {
		User user = new User(USER_3_NAME, USER_3_LABEL);
		STORAGE.addUser(user);
		GROUP_1_EXISTING.addMember(STORAGE, user);
		
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).contains(new UserId(USER_3_NAME));
		
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER_3_TOKEN_WRAPPED);
		
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(new UserId(USER_3_NAME), USER_3_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(new User(USER_3_NAME, USER_3_LABEL));
		assertThat(STORAGE.getAllGroups()).hasSize(1); // Pre-existing group 
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).doesNotContain(new UserId(USER_3_NAME));
	}

	@AfterAll
	public static void afterAll() {
		OIDC_SERVER.stop();
	}

	private static class TestRealm extends OIDCResourceOwnerPasswordCredentialRealm {

		public TestRealm(MetaStorage storage, OIDCResourceOwnerPasswordCredentialRealmFactory config) {
			super(storage, config);
		}

	}
}
