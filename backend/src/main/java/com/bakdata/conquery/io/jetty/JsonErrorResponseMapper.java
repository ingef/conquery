package com.bakdata.conquery.io.jetty;

import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.dropwizard.jersey.errors.ErrorMessage;

public class JsonErrorResponseMapper {

	//TODO del?
	public static Response toResponse(Response response) {
		ErrorMessage errorMessage = buildErrorMessage(response);
		return Response.fromResponse(response)
			.type(MediaType.APPLICATION_JSON_TYPE)
			.entity(errorMessage)
			.build();
	}

	private static ErrorMessage buildErrorMessage(Response response) {
		int status = response.getStatus();
		String message = Optional.ofNullable(response.getEntity())
				.map(Object::toString)
				.orElse(null);
		return new ErrorMessage(status, message);
	}

}
