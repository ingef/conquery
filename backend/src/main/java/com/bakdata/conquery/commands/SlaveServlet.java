package com.bakdata.conquery.commands;

import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.Workers;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.servlet.ServletContainer;

public class SlaveServlet {

	private DropwizardResourceConfig jerseyConfig;

	public void register(String label, Environment environment, ConqueryConfig config, Workers workers) {
		jerseyConfig = new DropwizardResourceConfig(environment.metrics());
		jerseyConfig.setUrlPattern("/" + label);
		RESTServer.configure(config, jerseyConfig);

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		environment.admin().addServlet(label, servletContainerHolder.getContainer()).addMapping(String.format("/%s/*", label));
		jerseyConfig.register(new JacksonMessageBodyProvider(environment.getObjectMapper()));

		jerseyConfig.register(new WorkerAPI(workers));

	}
}
