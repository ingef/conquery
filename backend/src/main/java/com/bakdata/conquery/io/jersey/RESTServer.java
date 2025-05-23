package com.bakdata.conquery.io.jersey;

import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.CachingFilter;
import com.bakdata.conquery.io.jetty.ConqueryErrorExceptionMapper;
import com.bakdata.conquery.io.jetty.ConqueryJsonExceptionMapper;
import com.bakdata.conquery.io.jetty.IllegalArgumentExceptionMapper;
import com.bakdata.conquery.io.jetty.JsonValidationExceptionMapper;
import com.bakdata.conquery.io.jetty.NoSuchElementExceptionMapper;
import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.auth.web.AuthenticationExceptionMapper;
import com.bakdata.conquery.models.auth.web.AuthorizationExceptionMapper;
import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.views.common.ViewRenderExceptionMapper;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.server.ResourceConfig;

@UtilityClass
public class RESTServer {

	public static void configure(ConqueryConfig config, ResourceConfig jersey) {
		// Bind User class to REST authentication
		jersey.register(new AuthValueFactoryProvider.Binder<>(Subject.class));
		//change exception mapper behavior because of JERSEY-2437
		//https://github.com/eclipse-ee4j/jersey/issues/2709
		((DefaultServerFactory) config.getServerFactory()).setRegisterDefaultExceptionMappers(false);
		// Register custom mapper
		jersey.register(new AuthenticationExceptionMapper());
		jersey.register(new AuthorizationExceptionMapper());
		jersey.register(JsonValidationExceptionMapper.class);
		jersey.register(ViewRenderExceptionMapper.class);
		jersey.register(NoSuchElementExceptionMapper.class);
		jersey.register(IllegalArgumentExceptionMapper.class);
		// default Dropwizard's exception mappers
		jersey.register(new ConqueryErrorExceptionMapper());
		jersey.register(ConqueryJsonExceptionMapper.class);
		jersey.register(new LoggingExceptionMapper<Throwable>() {});
		jersey.register(new EarlyEofExceptionMapper());
		//allow cross origin
		if(config.getApi().isAllowCORSRequests()) {
			jersey.register(CORSResponseFilter.class);
			jersey.register(new CORSPreflightRequestFilter());
		}
		//disable all browser caching if not expressly wanted
		jersey.register(CachingFilter.class);
		jersey.register(LocaleFilter.class);
	}
}
