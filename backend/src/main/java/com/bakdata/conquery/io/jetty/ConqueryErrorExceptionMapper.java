package com.bakdata.conquery.io.jetty;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.bakdata.conquery.models.error.ConqueryError;

public class ConqueryErrorExceptionMapper implements ExceptionMapper<ConqueryError> {
    @Override
    public Response toResponse(ConqueryError exception) {
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(exception)
                .build();
    }
}
