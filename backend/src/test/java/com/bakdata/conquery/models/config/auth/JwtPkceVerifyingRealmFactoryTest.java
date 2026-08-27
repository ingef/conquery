package com.bakdata.conquery.models.config.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.Test;

class JwtPkceVerifyingRealmFactoryTest {

	@Test
	void shouldAcceptCallbackUriFromAuthorizationRequest() {
		final JwtPkceVerifyingRealmFactory factory = new JwtPkceVerifyingRealmFactory();
		final URI callbackUri = URI.create("https://example.com/admin-ui");
		final URI returnUri = URI.create("/admin-ui/users?filter=active");
		final String state = factory.registerAuthorizationRequest(callbackUri, returnUri);

		assertEquals(
				new JwtPkceVerifyingRealmFactory.PendingAuthorizationRequest(callbackUri, returnUri),
				factory.validateAndConsumeAuthorizationRequest(state, callbackUri)
		);
	}

	@Test
	void shouldRejectCallbackUriNotUsedForAuthorizationRequest() {
		final JwtPkceVerifyingRealmFactory factory = new JwtPkceVerifyingRealmFactory();
		final String state = factory.registerAuthorizationRequest(
				URI.create("https://example.com/admin-ui"),
				URI.create("/admin-ui/users")
		);

		assertThrows(
				BadRequestException.class,
				() -> factory.validateAndConsumeAuthorizationRequest(state, URI.create("https://attacker.example/admin-ui"))
		);
	}

	@Test
	void shouldRejectMissingOrReusedState() {
		final JwtPkceVerifyingRealmFactory factory = new JwtPkceVerifyingRealmFactory();
		final URI callbackUri = URI.create("https://example.com/admin-ui");
		final String state = factory.registerAuthorizationRequest(callbackUri, URI.create("/admin-ui/users"));
		factory.validateAndConsumeAuthorizationRequest(state, callbackUri);

		assertThrows(BadRequestException.class, () -> factory.validateAndConsumeAuthorizationRequest(null, callbackUri));
		assertThrows(BadRequestException.class, () -> factory.validateAndConsumeAuthorizationRequest(state, callbackUri));
	}

	@Test
	void shouldCreateRootRelativeReturnUri() {
		assertEquals(
				URI.create("/admin-ui/users/123?tab=permissions"),
				JwtPkceVerifyingRealmFactory.toRootRelativeUri(URI.create("https://example.com/admin-ui/users/123?tab=permissions"))
		);
		assertThrows(
				BadRequestException.class,
				() -> JwtPkceVerifyingRealmFactory.toRootRelativeUri(URI.create("https://example.com//attacker.example/path"))
		);
	}
}
