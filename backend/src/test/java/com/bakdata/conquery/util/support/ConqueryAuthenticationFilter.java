package com.bakdata.conquery.util.support;

import java.util.function.Supplier;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

/**
 * Simple filter for http client in test to provide authentication information.
 * Skips, if the request had an {@link HttpHeaders#AUTHORIZATION} already set.
 * @param tokenSupplier Supplier that provides a (fresh) token for each request.
 */
record ConqueryAuthenticationFilter(Supplier<String> tokenSupplier) implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext requestContext) {
		// If none set to provided token
		if (requestContext.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
			return;
		}

		String token = tokenSupplier.get();
		requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
	}
}
