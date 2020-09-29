package com.bakdata.conquery.io.jetty;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@Provider
@Slf4j
public class ConqueryJsonExceptionMapper extends LoggingExceptionMapper<JsonProcessingException> {

	@Context
	private HttpServletRequest request;
	
    @Override
    public Response toResponse(JsonProcessingException exception) {
        if (exception instanceof JsonGenerationException || exception instanceof InvalidDefinitionException) {
            return super.toResponse(exception);
        }

        log.debug("Unable to process JSON in request '"+request.getRequestURL()+"'", exception);

        final String message = exception.getOriginalMessage();
        final ErrorMessage errorMessage = new ErrorMessage(Response.Status.BAD_REQUEST.getStatusCode(),
                "Unable to process JSON", message);
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(errorMessage)
                .build();
    }
}