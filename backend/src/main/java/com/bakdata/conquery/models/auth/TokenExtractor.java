package com.bakdata.conquery.models.auth;

import javax.ws.rs.container.ContainerRequestContext;

import com.bakdata.conquery.io.cps.CPSBase;

public interface TokenExtractor {
	ConqueryToken extract(ContainerRequestContext requestContext);
}
