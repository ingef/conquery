package com.bakdata.conquery.io.jetty;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Response;

import com.google.common.net.HttpHeaders;
import org.eclipse.jetty.http.HttpMethod;

@PreMatching
/*
 * We need this filter to be executed before the authentication (@Priority=1000).
 */
@Priority(900)
public class CORSPreflightRequestFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		// check if it is an OPTIONS request
		if (!requestContext.getMethod().equals(HttpMethod.OPTIONS.asString())) {
			//  Its no preflight -> proceed with the next filter
			return;
		}

		// Check for CORS
		if (requestContext.getHeaderString(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) == null) {
			return;
		}
		
		// Preflight are not authenticated, so answer right away with an 204
		// (https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods/OPTIONS)
		requestContext.abortWith(Response.noContent().build());
	}

}
