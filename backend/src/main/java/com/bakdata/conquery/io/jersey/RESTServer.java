package com.bakdata.conquery.io.jersey;

import com.bakdata.conquery.io.jackson.PathParamInjector;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.CachingFilter;
import com.bakdata.conquery.io.jetty.ConqueryJsonExceptionMapper;
import com.bakdata.conquery.io.jetty.JsonValidationExceptionMapper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.web.AuthenticationExceptionMapper;
import com.bakdata.conquery.models.auth.web.AuthorizationExceptionMapper;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.server.ResourceConfig;

@UtilityClass
public class RESTServer {

	public static void configure(ServerFactory serverFactory, ResourceConfig jersey, boolean allowCORSRequests) {
		// Bind User class to REST authentication
		jersey.register(new AuthValueFactoryProvider.Binder<>(User.class));
		//change exception mapper behavior because of JERSEY-2437
		//https://github.com/eclipse-ee4j/jersey/issues/2709
		((DefaultServerFactory) serverFactory).setRegisterDefaultExceptionMappers(false);
		// Register custom mapper
		jersey.register(new AuthenticationExceptionMapper());
		jersey.register(new AuthorizationExceptionMapper());
		jersey.register(JsonValidationExceptionMapper.class);
		// default Dropwizard's exception mappers
		jersey.register(new LoggingExceptionMapper<Throwable>() {});
		jersey.register(ConqueryJsonExceptionMapper.class);
		jersey.register(new EarlyEofExceptionMapper());
		//allow cross origin
		if(allowCORSRequests) {
			jersey.register(CORSResponseFilter.class);
		}
		//disable all browser caching if not expressly wanted
		jersey.register(CachingFilter.class);
		
		jersey.register(new PathParamInjector());
	}
}
