package com.bakdata.conquery.commands;

import org.glassfish.jersey.server.ResourceConfig;

import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.CachingFilter;
import com.bakdata.conquery.io.jetty.JsonValidationExceptionMapper;
import com.bakdata.conquery.models.config.ConqueryConfig;

import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.server.DefaultServerFactory;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RESTServer {

	public static void configure(ConqueryConfig config, ResourceConfig jersey) {
		//change exception mapper behavior because of JERSEY-2437
		((DefaultServerFactory) config.getServerFactory()).setRegisterDefaultExceptionMappers(false);
		// Register custom mapper
		jersey.register(new JsonValidationExceptionMapper());
		// default Dropwizard's exception mappers
		jersey.register(new LoggingExceptionMapper<Throwable>() {});
		jersey.register(new JsonProcessingExceptionMapper(true));
		jersey.register(new EarlyEofExceptionMapper());
		//allow cross origin
		jersey.register(CORSResponseFilter.class);
		//disable all browser caching if not expressly wanted
		jersey.register(CachingFilter.class);
	}
}
