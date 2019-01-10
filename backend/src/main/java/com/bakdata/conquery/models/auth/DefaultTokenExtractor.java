package com.bakdata.conquery.models.auth;

import javax.ws.rs.container.ContainerRequestContext;

public class DefaultTokenExtractor implements TokenExtractor {
	private static final String DEFAULT_CREDENTIALS = "injected_credentials";
	@Override
	public ConqueryToken extract(ContainerRequestContext requestContext) {
		return new ConqueryToken(DEFAULT_CREDENTIALS);
	}

}
