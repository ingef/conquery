package com.bakdata.conquery.io.jetty;

import com.bakdata.conquery.models.error.ConqueryError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ConqueryErrorExceptionMapper implements ExceptionMapper<ConqueryError> {
    @Override
    public Response toResponse(ConqueryError exception) {
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(exception)
                .build();
    }
}
