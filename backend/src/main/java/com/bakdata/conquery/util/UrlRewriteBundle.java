package com.bakdata.conquery.util;

import javax.servlet.FilterRegistration;

import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

/**
 * The URL rewriting (in a Dropwizard application) to allow for use of React
 * Router's BrowserHistory. The purpose is to allow for using HTML5 URLs
 * (without the #).
 */
public class UrlRewriteBundle implements ConfiguredBundle<ConqueryConfig> {

	public static final String DEFAULT_CONF_PATH = "/urlrewrite.xml";

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		FilterRegistration.Dynamic registration = environment.servlets().addFilter("UrlRewriteFilter", new UrlRewriteFilter());
		registration.addMappingForUrlPatterns(null, true, "/*");
		registration.setInitParameter("confPath", DEFAULT_CONF_PATH);
		registration.setInitParameter("logLevel", "slf4j");
	}

	@Override
	public void initialize(Bootstrap<?> bootstrap) {
		/* nothing */
	}
}