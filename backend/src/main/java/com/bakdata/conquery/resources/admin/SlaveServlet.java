package com.bakdata.conquery.resources.admin;

import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.resources.admin.slave.WorkerAPIResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Organizational class to provide a single implementation point for configuring
 * the admin servlet container and registering resources for it.
 */
@Getter
@Slf4j
public class SlaveServlet {

	private DropwizardResourceConfig jerseyConfig;

	public void register(ConqueryConfig config, Environment environment, final Workers workers) {
		jerseyConfig = new DropwizardResourceConfig(environment.metrics());
		jerseyConfig.setUrlPattern("/slave");

		config.getSlaveServlet().configure(environment);

		RESTServer.configure(config.getSlaveServlet(), jerseyConfig, config.getApi().isAllowCORSRequests());

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		environment.admin().addServlet("slave", servletContainerHolder.getContainer()).addMapping("/slave/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(environment.getObjectMapper()));


		// inject required services
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(workers).to(Workers.class);
			}
		});

		jerseyConfig.register(WorkerAPIResource.class);
	}
}
