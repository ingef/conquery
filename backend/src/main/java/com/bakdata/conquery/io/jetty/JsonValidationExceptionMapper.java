package com.bakdata.conquery.io.jetty;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.inject.Provider;

import org.glassfish.jersey.server.ContainerRequest;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import io.dropwizard.jersey.errors.ErrorMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class JsonValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

	private final Provider<ContainerRequest> requestContext;
	
	@Override
	public Response toResponse(ConstraintViolationException exception) {
		log.warn("Json Validation error on {} {}:\nproblem:{}",
			requestContext.get().getMethod(),
			requestContext.get().getRequestUri(),
			Joiner.on('\n').join(Iterables.transform(exception.getConstraintViolations(), this::constraintMessageBuilder))
		);		
		return Response.status(422)
						.type(MediaType.APPLICATION_JSON_TYPE)
						.entity(new ErrorMessage(422, Joiner.on(" AND ").join(Iterables.transform(exception.getConstraintViolations(), this::constraintMessageBuilder))))
						.build();
	}
	
	private <T> String constraintMessageBuilder(ConstraintViolation<T> violation) {
		return violation.getPropertyPath().toString() + " " + violation.getMessage();
	}
}
