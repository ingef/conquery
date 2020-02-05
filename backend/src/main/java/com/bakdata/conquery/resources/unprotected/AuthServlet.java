package com.bakdata.conquery.resources.unprotected;

import java.util.Collections;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.auth.AuthorizationController;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import org.apache.shiro.realm.Realm;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * This servlet is used to register resources that provide authentication
 * services for the user (e.g. login and token retrieval) but that require no
 * authentication. These are registered under the '/auth' path of the servlet.
 */
public class AuthServlet {

	/**
	 * Marker interface for classes that provide admin UI functionality. Classes
	 * have to register as CPSType=AdminServletResource and will then be able to be
	 * registered in the admin jerseyconfig.
	 */
	@CPSBase
	public interface AuthUnprotectedResourceProvider {

		void registerAuthenticationResources(DropwizardResourceConfig jerseyConfig);
	}

	public void register(AuthorizationController controller,
		MetricRegistry metrics,
		ConqueryConfig config,
		ServletEnvironment servletEnvironment,
		ObjectMapper objectMapper ) {
		
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(metrics);
		jerseyConfig.setUrlPattern("/auth");

		RESTServer.configure(config, jerseyConfig);

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		servletEnvironment.addServlet("auth", servletContainerHolder.getContainer()).addMapping("/auth/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(objectMapper));
		// freemarker support
		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer();
		freemarker.configure(Freemarker.asMap());
		jerseyConfig.register(new ViewMessageBodyWriter(metrics, Collections.singleton(freemarker)));

		jerseyConfig.register(new CORSPreflightRequestFilter());
		jerseyConfig.register(CORSResponseFilter.class);

		// Scan realms if they need to add resources
		for (Realm realm : controller.getRealms()) {
			if (realm instanceof AuthUnprotectedResourceProvider) {
				((AuthUnprotectedResourceProvider) realm).registerAuthenticationResources(jerseyConfig);
			}
		}
	}
}
