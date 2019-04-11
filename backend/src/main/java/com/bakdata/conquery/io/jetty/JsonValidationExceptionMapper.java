package com.bakdata.conquery.io.jetty;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import io.dropwizard.jersey.errors.ErrorMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j @Provider
public class JsonValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

	@Override
	public Response toResponse(ConstraintViolationException exception) {

		return Response.status(422)
						.type(MediaType.APPLICATION_JSON_TYPE)
						.entity(new ErrorMessage(422, Joiner.on(" AND ").join(Iterables.transform(exception.getConstraintViolations(), this::constraintMessageBuilder))))
						.build();
	}
	
	private <T> String constraintMessageBuilder(ConstraintViolation<T> violation) {
		return violation.getPropertyPath().toString() + " " + violation.getMessage();
	}
}
