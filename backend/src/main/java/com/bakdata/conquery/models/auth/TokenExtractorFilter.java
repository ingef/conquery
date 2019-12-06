package com.bakdata.conquery.models.auth;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import com.bakdata.conquery.util.io.ConqueryMDC;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@PreMatching

/*
 * Priorities.AUTHENTICATION = 1000 we need this filter to be executed before
 * the authentication and CORSPreflight.
 */
@Priority(800)
public class TokenExtractorFilter implements ContainerRequestFilter {

	private final TokenExtractor tokenExtractor;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// Set the log to indicate, that the user was not authorized yet
		ConqueryMDC.setLocation("UNAUTHORIZED_USER");

		ConqueryToken credentials = Objects
			.requireNonNull(tokenExtractor.extract(requestContext), "The TokenExtractor must return an object.");
		ConquerySecurityContext ctx = new ConquerySecurityContext(requestContext.getSecurityContext(), credentials);
		requestContext.setSecurityContext(ctx);

	}

}
