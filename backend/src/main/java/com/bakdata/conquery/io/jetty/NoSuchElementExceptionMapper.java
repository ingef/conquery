package com.bakdata.conquery.io.jetty;

import java.util.NoSuchElementException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {
	@Override
	public Response toResponse(NoSuchElementException exception) {
		log.warn("Uncaught NoSuchElementException", exception);
		return Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON_TYPE).entity(exception.getMessage()).build();
	}
}
