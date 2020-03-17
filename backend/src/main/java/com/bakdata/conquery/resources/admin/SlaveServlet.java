package com.bakdata.conquery.resources.admin;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.bakdata.conquery.io.jersey.RESTServer;
import com.bakdata.conquery.io.jetty.JettyConfigurationUtil;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.resources.admin.slave.WorkerAPIResource;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import io.dropwizard.jersey.setup.JerseyContainerHolder;
import io.dropwizard.setup.AdminEnvironment;
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


	public DropwizardResourceConfig createServlet(ConqueryConfig config, Environment environment) {

		DropwizardResourceConfig jerseyConfig = new DropwizardResourceConfig(environment.metrics());
		jerseyConfig.setUrlPattern("/admin");

		RESTServer.configure(config.getServerFactory(), jerseyConfig, config.getApi().isAllowCORSRequests());

		JettyConfigurationUtil.configure(jerseyConfig);
		JerseyContainerHolder servletContainerHolder = new JerseyContainerHolder(new ServletContainer(jerseyConfig));

		environment.admin().addServlet("admin", servletContainerHolder.getContainer()).addMapping("/admin/*");

		jerseyConfig.register(new JacksonMessageBodyProvider(environment.getObjectMapper()));

		return jerseyConfig;
	}

	public void register(final Workers workers, AdminEnvironment adminEnvironment) {

		adminEnvironment.addServlet("slave", this);

		// inject required services
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(workers).to(Workers.class);
			}
		});

		jerseyConfig.register(WorkerAPIResource.class);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {

	}

	@Override
	public ServletConfig getServletConfig() {
		return null;
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

	}

	@Override
	public String getServletInfo() {
		return null;
	}

	@Override
	public void destroy() {

	}
}
