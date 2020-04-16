package com.bakdata.conquery.models.auth.web;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;

/**
 * Interceptor for exceptions that were thrown during the authentication procedure of Shiro.
 */
@Slf4j
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {

	@Override
	public Response toResponse(AuthenticationException exception) {
		log.warn("Shiro failed to authenticate the request. See the following trace:", exception);
		return Response.status(Response.Status.UNAUTHORIZED)
			.type(MediaType.APPLICATION_JSON_TYPE)
			.entity("An authentication error occured. The error has been logged")
			.build();
	}

}