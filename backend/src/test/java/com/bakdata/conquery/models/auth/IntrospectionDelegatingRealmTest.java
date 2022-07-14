package com.bakdata.conquery.models.auth;

import static com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealmFactory.CONFIDENTIAL_CREDENTIAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

import java.util.Map;

import javax.validation.Validator;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealm;
import com.bakdata.conquery.models.auth.oidc.IntrospectionDelegatingRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.NonPersistentStoreFactory;
import io.dropwizard.validation.BaseValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.BearerToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.MediaType;

@Slf4j
public class IntrospectionDelegatingRealmTest {

	private static final MetaStorage STORAGE = new NonPersistentStoreFactory().createMetaStorage();
	private static final IntrospectionDelegatingRealmFactory CONFIG = new IntrospectionDelegatingRealmFactory();
	private static final Validator VALIDATOR = BaseValidator.newValidator();
	private static final TestRealm REALM = new TestRealm(STORAGE, CONFIG);

	// User 1
	private static final String USER_1_NAME = "test_name1";
	private static User USER_1 = new User(USER_1_NAME, USER_1_NAME, STORAGE);
	private static final String USER_1_PASSWORD = "test_password1";
	private static final String USER_1_TOKEN = JWT.create().withClaim("name", USER_1_NAME).sign(Algorithm.HMAC256("secret"));;
	private static final BearerToken USER1_TOKEN_WRAPPED = new BearerToken(USER_1_TOKEN);

	// User 2
	private static final String USER_2_NAME = "test_name2";
	private static User USER_2 = new User(USER_2_NAME, USER_2_NAME, STORAGE);
	private static final String USER_2_LABEL = "test_label2";
	private static final String USER_2_TOKEN = JWT.create().withClaim("name", USER_2_NAME).sign(Algorithm.HMAC256("secret"));;
	private static final BearerToken USER_2_TOKEN_WRAPPED = new BearerToken(USER_2_TOKEN);

	// User 3 existing
	private static final String USER_3_NAME = "test_name3";
	private static User USER_3 = new User(USER_3_NAME, USER_3_NAME, STORAGE);
	private static final String USER_3_LABEL = "test_label3";
	private static final String USER_3_TOKEN = JWT.create().withClaim("name", USER_3_NAME).sign(Algorithm.HMAC256("secret"));;
	private static final BearerToken USER_3_TOKEN_WRAPPED = new BearerToken(USER_3_TOKEN);
	// Groups
	private static final String GROUPNAME_1 = "group1";
	private static final Group GROUP_1_EXISTING = new Group(GROUPNAME_1, GROUPNAME_1, STORAGE);
	private static final String GROUPNAME_2 = "group2"; // Group is created during test
	
	private static OIDCMockServer OIDC_SERVER;

	@BeforeAll
	public static void beforeAll() {
		initRealmConfig();
		initOIDCServer();
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
		CONFIG.setRealm(OIDCMockServer.REALM_NAME);
		CONFIG.setResource("test_cred");
		CONFIG.setCredentials(Map.of(CONFIDENTIAL_CREDENTIAL, "test_cred"));
		CONFIG.setAuthServerUrl(OIDCMockServer.MOCK_SERVER_URL);

		ValidatorHelper.failOnError(log, VALIDATOR.validate(CONFIG));
	}

	private static void initOIDCServer() {
		OIDC_SERVER = new OIDCMockServer();

		OIDC_SERVER.init( (server) -> {


		// Mock username-password-for-token exchange
		server.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", OIDCMockServer.REALM_NAME))
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
		server.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", OIDCMockServer.REALM_NAME)))
			.respond(
				response().withStatusCode(HttpStatus.SC_FORBIDDEN).withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"error\" : \"Wrong username or password\""));
		
		// Mock token introspection
		// For USER 1
		server.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", OIDCMockServer.REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
				.withBody(params(param("token_type_hint", "access_token"), param("token", USER_1_TOKEN))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"username\" : \"" + USER_1_NAME + "\", \"active\": true}"));
		// For USER 2
		server.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", OIDCMockServer.REALM_NAME))
				.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
				.withBody(params(param("token_type_hint", "access_token"), param("token", USER_2_TOKEN))))
			.respond(
				response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
					.withBody("{\"username\" : \"" + USER_2_NAME + "\",\"name\" : \"" + USER_2_LABEL + "\", \"active\": true, \"groups\":[\"" + GROUPNAME_1 + "\",\"" + GROUPNAME_2 + "\"]}"));
		// For USER 3
		server.when(
			request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token/introspect", OIDCMockServer.REALM_NAME))
			.withHeaders(header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"))
			.withBody(params(param("token_type_hint", "access_token"), param("token", USER_3_TOKEN))))
		.respond(
			response().withContentType(MediaType.APPLICATION_JSON_UTF_8)
			.withBody("{\"username\" : \"" + USER_3_NAME + "\",\"name\" : \"" + USER_3_LABEL + "\", \"active\": true}"));

		});
	}

	@Test
	public void tokenIntrospectionSimpleUserNew() {
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER1_TOKEN_WRAPPED);
		
		assertThat(info)
				.usingRecursiveComparison()
				.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
				.ignoringFieldsOfTypes(User.ShiroUserAdapter.class)
			.isEqualTo(new ConqueryAuthenticationInfo(USER_1, USER1_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(new User(USER_1_NAME, USER_1_NAME, STORAGE));
	}
	
	@Test
	public void tokenIntrospectionSimpleUserExisting() {
		STORAGE.addUser(USER_1);
		
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER1_TOKEN_WRAPPED);
		
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(USER_1, USER1_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(USER_1);
	}
	
	@Test
	public void tokenIntrospectionGroupedUser() {
		STORAGE.addUser(USER_2);

		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER_2_TOKEN_WRAPPED);

		final ConqueryAuthenticationInfo expected = new ConqueryAuthenticationInfo(USER_2, USER_2_TOKEN_WRAPPED, REALM, true);
		assertThat(info)
			.usingRecursiveComparison()
			.isEqualTo(expected);
		assertThat(STORAGE.getAllUsers()).containsOnly(USER_2);
		assertThat(STORAGE.getAllGroups()).hasSize(2); // Pre-existing group and a second group that has been added in the process
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).contains(new UserId(USER_2_NAME));
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_2)).getMembers()).contains(new UserId(USER_2_NAME));
	}
	
	@Test
	public void tokenIntrospectionGroupedUserRemoveGroupMapping() {
		STORAGE.addUser(USER_3);
		GROUP_1_EXISTING.addMember(USER_3);
		
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).contains(new UserId(USER_3_NAME));
		
		AuthenticationInfo info = REALM.doGetAuthenticationInfo(USER_3_TOKEN_WRAPPED);
		
		assertThat(info)
			.usingRecursiveComparison()
			.ignoringFields(ConqueryAuthenticationInfo.Fields.credentials)
			.isEqualTo(new ConqueryAuthenticationInfo(USER_3, USER_3_TOKEN_WRAPPED, REALM, true));
		assertThat(STORAGE.getAllUsers()).containsOnly(USER_3);
		assertThat(STORAGE.getAllGroups()).hasSize(1); // Pre-existing group 
		assertThat(STORAGE.getGroup(new GroupId(GROUPNAME_1)).getMembers()).doesNotContain(new UserId(USER_3_NAME));
	}

	@AfterAll
	public static void afterAll() {
		OIDC_SERVER.deinit();
	}

	private static class TestRealm extends IntrospectionDelegatingRealm {

		public TestRealm(MetaStorage storage, IntrospectionDelegatingRealmFactory config) {
			super(storage, config);
		}

	}
}
