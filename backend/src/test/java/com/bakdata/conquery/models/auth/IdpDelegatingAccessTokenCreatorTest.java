package com.bakdata.conquery.models.auth;

import static com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory.CONFIDENTIAL_CREDENTIAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockserver.model.Header.header;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.Parameter.param;
import static org.mockserver.model.ParameterBody.params;

import java.util.Map;

import javax.validation.Validator;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.bakdata.conquery.models.auth.oidc.passwordflow.IdpDelegatingAccessTokenCreator;
import com.bakdata.conquery.models.config.auth.IntrospectionDelegatingRealmFactory;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import io.dropwizard.validation.BaseValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;

@Slf4j
public class IdpDelegatingAccessTokenCreatorTest {

	private static final OIDCMockServer OIDC_SERVER =  new OIDCMockServer();
	private static final IntrospectionDelegatingRealmFactory CONFIG = new IntrospectionDelegatingRealmFactory();
	private static final Validator VALIDATOR = BaseValidator.newValidator();

	// User 1
	private static final String USER_1_NAME = "test_name1";
	private static final String USER_1_PASSWORD = "test_password1";
	private static final String USER_1_TOKEN = JWT.create().withClaim("name", USER_1_NAME).sign(Algorithm.HMAC256("secret"));

	private static IdpDelegatingAccessTokenCreator idpDelegatingAccessTokenCreator;


	@BeforeAll
	public static void beforeAll() {
		initOIDCServer();

		initRealmConfig();

		idpDelegatingAccessTokenCreator = new IdpDelegatingAccessTokenCreator(CONFIG);
	}


	private static void initRealmConfig() {
		CONFIG.setRealm(OIDCMockServer.REALM_NAME);
		CONFIG.setResource("test_cred");
		CONFIG.setCredentials(Map.of(CONFIDENTIAL_CREDENTIAL, "test_cred"));
		CONFIG.setAuthServerUrl(OIDCMockServer.MOCK_SERVER_URL);

		ValidatorHelper.failOnError(log, VALIDATOR.validate(CONFIG));
	}

	private static void initOIDCServer() {

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
							response()
									.withBody(JsonBody.json(
											new Object() {
												@Getter
												String token_type = "Bearer";
												@Getter
												String access_token = USER_1_TOKEN;
											}
									)));
			// Block other exchange requests (this has a lower prio than the above)
			server.when(
					request().withMethod("POST").withPath(String.format("/realms/%s/protocol/openid-connect/token", OIDCMockServer.REALM_NAME)))
					.respond(
							response().withStatusCode(HttpStatus.SC_FORBIDDEN).withContentType(MediaType.APPLICATION_JSON_UTF_8)
									.withBody("{\"error\" : \"Wrong username or password\""));

		});
	}

	@Test
	public void vaildUsernamePassword() {
		String jwt = idpDelegatingAccessTokenCreator.createAccessToken(USER_1_NAME, USER_1_PASSWORD.toCharArray());

		assertThat(jwt).isEqualTo(USER_1_TOKEN);
	}

	@Test
	public void invaildUsernamePassword() {
		log.info("This test will print an Error below.");
		assertThatThrownBy(
				() -> idpDelegatingAccessTokenCreator.createAccessToken(USER_1_NAME, "bad_password".toCharArray()))
				.isInstanceOf(IllegalStateException.class);
	}


	@AfterAll
	public static void afterAll() {
		OIDC_SERVER.deinit();
	}


}
