package com.bakdata.conquery.models.auth;

import javax.ws.rs.container.ContainerRequestContext;

public interface TokenExtractor {
	ConqueryToken extract(ContainerRequestContext requestContext);
}
