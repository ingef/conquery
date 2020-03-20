package com.bakdata.conquery.util;

import java.util.Collections;

import com.bakdata.conquery.io.freemarker.Freemarker;
import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.CORSPreflightRequestFilter;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.jetty.setup.ServletEnvironment;
import io.dropwizard.views.ViewMessageBodyWriter;
import io.dropwizard.views.freemarker.FreemarkerViewRenderer;
import lombok.experimental.UtilityClass;
import org.glassfish.jersey.servlet.ServletContainer;

@UtilityClass
public class ServletUtils {
	/**
	 * Prepares the general configuration with resources and settings that are valid
	 * for both, api and admin, endpoints.
	 *
	 * @return
	 */
	public static DropwizardResourceConfig createServlet(String servletName, MetricRegistry metrics, ConqueryConfig config, ServletEnvironment servletEnvironment, ObjectMapper objectMapper) {
		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(metrics);
		jerseyConfig.setUrlPattern("/" + servletName);

		RESTServer.configure(jerseyConfig, config.getServerFactory(), config.getApi().isAllowCORSRequests());

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		servletEnvironment.addServlet(servletName, servletContainerHolder.getContainer()).addMapping("/" + servletName + "/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(objectMapper));
		// freemarker support
		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer();
		freemarker.configure(Freemarker.asMap());
		jerseyConfig.register(new ViewMessageBodyWriter(metrics, Collections.singleton(freemarker)));

		jerseyConfig.register(CORSPreflightRequestFilter.class);
		jerseyConfig.register(CORSResponseFilter.class);
		return jerseyConfig;
	}

//	public void dasd(MasterCommand masterCommand){
//		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(masterCommand.getEnvironment().metrics());
//		jerseyConfig.setUrlPattern("/admin");
//
//		RESTServer.configure(jerseyConfig, masterCommand.getConfig(), true);
//
//		JettyConfigurationUtil.configure(jerseyConfig);
//		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));
//
//		masterCommand.getEnvironment().admin().addServlet("admin", servletContainerHolder.getContainer()).addMapping("/admin/*");
//
//		jerseyConfig.register(new JacksonMessageBodyProvider(masterCommand.getEnvironment().getObjectMapper()));
//		// freemarker support
//		FreemarkerViewRenderer freemarker = new FreemarkerViewRenderer();
//		freemarker.configure(Freemarker.asMap());
//		jerseyConfig.register(new ViewMessageBodyWriter(masterCommand.getEnvironment().metrics(), Collections.singleton(freemarker)));
//
//	}
}
