package com.bakdata.conquery.io.jetty;

import com.bakdata.conquery.models.config.ConqueryConfig;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Environment;

public class JettyConfigurationUtil {

	public static void configure(ConqueryConfig config, Environment environment) {
		//change exception mapper behavior because of JERSEY-2437
		((DefaultServerFactory)config.getServerFactory()).setRegisterDefaultExceptionMappers(false);
		
		configure(environment.jersey().getResourceConfig());
	}

	public static void configure(DropwizardResourceConfig resourceConfig) {
		// Register custom mapper
		resourceConfig.register(new JsonValidationExceptionMapper());
		// default Dropwizard's exception mappers
		resourceConfig.register(new LoggingExceptionMapper<Throwable>() {});
		resourceConfig.register(new JsonProcessingExceptionMapper(true));
		resourceConfig.register(new EarlyEofExceptionMapper());
		//allow cross origin
		/*if(config.getApi().isAllowCORSRequests())
			resourceConfig.register(CORSResponseFilter.class);*/
		//disable all browser caching if not expressly wanted
		resourceConfig.register(CachingFilter.class);
	}
}
