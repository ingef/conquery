package com.bakdata.conquery.models.exceptions;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.model.Invocable;
import org.slf4j.Logger;

import io.dropwizard.jersey.validation.ConstraintMessage;

public interface ValidatorHelper {

	public static void failOnError(Logger log, Set<? extends ConstraintViolation<?>> violations) throws JSONException {
		failOnError(log, violations, null);
	}
	
	public static void failOnError(Logger log, Set<? extends ConstraintViolation<?>> violations, String context) throws JSONException {
		List<String> violationMessages = violations
				.stream()
				.map( v->
					ConstraintMessage.getMessage(v,Invocable.create((Inflector<Request, Void>) data -> null)))
				.collect(Collectors.toList());
		
		failOnError(log, violationMessages, context);
	}
	
	public static void failOnError(Logger log, List<String> violations, String context) throws JSONException {
		if(violations.size()>0) {
			for(String v:violations) {
				log.error(v);
			}
			
			if(context!=null) {
				throw new JSONException("Failed with "+violations.size()+" errors in "+context+".");
			}
			else {
				throw new JSONException("Failed with "+violations.size()+" errors.");
			}
		}
	}

}
