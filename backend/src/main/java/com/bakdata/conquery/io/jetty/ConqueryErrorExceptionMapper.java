package com.bakdata.conquery.io.jetty;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.error.SimpleErrorInfo;

public class ConqueryErrorExceptionMapper implements ExceptionMapper<ConqueryError> {
    @Override
    public Response toResponse(ConqueryError exception) {
		SimpleErrorInfo plain = exception.asPlain();

		return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
					   .type(MediaType.APPLICATION_JSON_TYPE)
					   .entity(plain)
					   .build();
    }
}
