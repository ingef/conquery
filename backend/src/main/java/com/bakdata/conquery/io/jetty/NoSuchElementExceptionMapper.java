package com.bakdata.conquery.io.jetty;

import java.util.NoSuchElementException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {
	@Override
	public Response toResponse(NoSuchElementException exception) {
		log.trace("Uncaught NoSuchElementException", exception);
		return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(exception.getMessage()).build();
	}
}
