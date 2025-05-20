package com.bakdata.conquery.io.jetty;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.error.SimpleErrorInfo;
import com.bakdata.conquery.models.identifiable.IdResolvingException;
import io.dropwizard.jersey.errors.ErrorMessage;

public class ConqueryErrorExceptionMapper implements ExceptionMapper<ConqueryError> {
	@Override
	public Response toResponse(ConqueryError exception) {
		if (exception instanceof IdResolvingException idResolvingException) {
			ErrorMessage errorMessage = new ErrorMessage(
					Response.Status.NOT_FOUND.getStatusCode(),
					idResolvingException.getMessage(),
					null
			);

			return Response.status(Response.Status.NOT_FOUND)
						   .type(MediaType.APPLICATION_JSON_TYPE)
						   .entity(errorMessage)
						   .build();
		}

		SimpleErrorInfo plain = exception.asPlain();

		return Response.status(Response.Status.BAD_REQUEST)
					   .type(MediaType.APPLICATION_JSON_TYPE)
					   .entity(plain)
					   .build();
	}
}
