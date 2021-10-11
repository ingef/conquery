package com.bakdata.conquery.io.jetty;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Maps requests that caused an {@link IllegalArgumentException} to a 400 status. The illegal argument is usually a query-string element
 * that did not meet certain {@link com.google.common.base.Preconditions}.
 */
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
	@Override
	public Response toResponse(IllegalArgumentException exception) {
		return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
					   .type(MediaType.APPLICATION_JSON_TYPE)
					   .entity(exception.getMessage())
					   .build();
	}
}
