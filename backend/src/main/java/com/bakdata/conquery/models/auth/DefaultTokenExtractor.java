package com.bakdata.conquery.models.auth;

import javax.ws.rs.container.ContainerRequestContext;

/**
 * Default dummy implementation for the token extractor.
 *
 */
public class DefaultTokenExtractor implements TokenExtractor {
	private static final String DEFAULT_CREDENTIALS = "DEFAULT_CREDENTIALS";
	@Override
	public ConqueryToken extract(ContainerRequestContext requestContext) {
		return new ConqueryToken(DEFAULT_CREDENTIALS);
	}

}
