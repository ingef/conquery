package com.bakdata.conquery.models.auth.web;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;

/**
 * This mapper intercepts all {@link AuthorizationException}s that occur during a request.
 * It then logs the specific cause and returns a {@link Response.Status.UNAUTHORIZED} to the client.
 *
 */
@Slf4j
public class AuthorizationExceptionMapper implements ExceptionMapper<AuthorizationException> {

	@Override
	public Response toResponse(AuthorizationException exception) {
		log.warn("Shiro failed to authorize the request. See the following trace:", exception);
		return Response.status(Response.Status.FORBIDDEN)
			.type(MediaType.APPLICATION_JSON_TYPE)
			.entity("Not sufficient permissions to perform action: " + exception.getMessage())
			.build();
	}

}
