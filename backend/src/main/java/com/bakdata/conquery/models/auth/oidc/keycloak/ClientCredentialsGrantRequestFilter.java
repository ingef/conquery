package com.bakdata.conquery.models.auth.oidc.keycloak;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import lombok.extern.slf4j.Slf4j;

/**
 * Filter that handles the authentication transparently for the api.
 * It requests an access token upon first request through the client it is registered in.
 * It tracks the validity of the token and renews it as soon as a request is made after the
 * first half of its lifetime.
 *
 * @implNote Authentication errors can still occur e.g. a not expired token was revoked.
 */
@Slf4j
public class ClientCredentialsGrantRequestFilter implements ClientRequestFilter {

	private final Invocation tokenInvocation;

	private AccessTokenResponse accessToken;
	private Instant renewAfter;

	public ClientCredentialsGrantRequestFilter(String clientId, String clientSecret, URI tokenEndpoint) {
		Client client = ClientBuilder.newClient();

		tokenInvocation = client.target(tokenEndpoint)
								.request(MediaType.APPLICATION_JSON_TYPE).buildPost(Entity.form(ClientCredentials.create(clientId
						, clientSecret)));
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		AccessTokenResponse response = getActiveAccessToken();

		log.trace("Adding access token to request");
		requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, String.join(" ", response.token_type(), response.access_token()));

	}

	private AccessTokenResponse getActiveAccessToken() {
		Instant now = Instant.now();
		if (accessToken == null || now.isAfter(renewAfter)) {
			final AccessTokenResponse response = acquireFreshAccessToken();
			accessToken = response;
			// We want the token to be refreshed after half its time to live
			renewAfter = now.plus(response.expires_in() / 2, ChronoUnit.SECONDS);
		}

		return accessToken;
	}

	private AccessTokenResponse acquireFreshAccessToken() {
		log.info("Acquire new token");
		return tokenInvocation.invoke(AccessTokenResponse.class);
	}
}
