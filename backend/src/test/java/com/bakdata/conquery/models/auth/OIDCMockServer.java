package com.bakdata.conquery.models.auth;

import static org.junit.Assert.fail;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.function.Consumer;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;

@Slf4j
@UtilityClass
public class OIDCMockServer {

	public static final String REALM_NAME = "test_realm";

	public static void init(ClientAndServer server) {
		init(server, (_server) -> {});
	}

	public static void init(ClientAndServer server, Consumer<ClientAndServer> testMappings) {

		String mockServerUrl = "http://localhost:%d".formatted(server.getPort());
		// Mock well-known discovery endpoint (this is actually the output of keycloak)
		server.when(request().withMethod("GET").withPath("/realms/" + REALM_NAME + "/.well-known/uma2-configuration"))
				   .respond(
						response().withBody(
								JsonBody.json(
										new Object() {
											@Getter
											final String issuer = mockServerUrl;
											@Getter
											final String authorization_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/auth";
											@Getter
											final String token_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/token";
											@Getter
											final String introspection_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/token/introspect";
											@Getter
											final String end_session_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/logout";
											@Getter
											final String jwks_uri = mockServerUrl + "/realms/" + REALM_NAME + "/protocol/openid-connect/certs";
											@Getter
											final String[] grant_types_supported = {"authorization_code", "implicit", "refresh_token", "password", "client_credentials"};
											@Getter
											final String[] response_types_supported = {"code", "none", "id_token", "token", "id_token token", "code id_token", "code token", "code id_token token"};
											@Getter
											final String[] response_modes_supported = {"query", "fragment", "form_post"};
											@Getter
											final String registration_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/clients-registrations/openid-connect";
											@Getter
											final String[] token_endpoint_auth_methods_supported = {"private_key_jwt", "client_secret_basic", "client_secret_post", "tls_client_auth", "client_secret_jwt"};
											@Getter
											final String[] token_endpoint_auth_signing_alg_values_supported = {"PS384", "ES384", "RS384", "HS256", "HS512", "ES256", "RS256", "HS384", "ES512", "PS256", "PS512", "RS512"};
											@Getter
											final String[] scopes_supported = {"openid", "address", "email", "microprofile-jwt", "offline_access", "phone", "profile", "roles", "web-origins"};
											@Getter
											final String resource_registration_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/authz/protection/resource_set";
											@Getter
											final String permission_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/authz/protection/permission";
											@Getter
											final String policy_endpoint = mockServerUrl + "/realms/" + REALM_NAME + "/authz/protection/uma-policy";
										}
								)
						));

		// Register test provided mappings
		testMappings.accept(server);

		// At last (so it has the lowest priority): initialize a trap for debugging, that captures all unmapped requests
		server.when(request()).respond(new ExpectationResponseCallback() {

			@Override
			public HttpResponse handle(HttpRequest httpRequest) throws Exception {
				log.error(
						"{} on {}\n\t Headers: {}\n\tBody {}",
						httpRequest.getMethod(),
						httpRequest.getPath(),
						httpRequest.getHeaderList(),
						httpRequest.getBodyAsString()
				);
				fail("Trapped because request did not match. See log.");
				return null;
			}
		});
	}
}
