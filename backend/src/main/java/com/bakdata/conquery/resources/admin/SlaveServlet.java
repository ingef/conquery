package com.bakdata.conquery.resources.admin;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.resources.admin.slave.WorkerAPIResource;
import com.bakdata.conquery.util.ServletUtils;
import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Organizational class to provide a single implementation point for configuring
 * the admin servlet container and registering resources for it.
 */
@Getter
@Slf4j
public class SlaveServlet {

	private DropwizardResourceConfig jerseyConfig;

	public void register(SlaveCommand slaveCommand, Environment environment, ConqueryConfig config) {

		jerseyConfig =
				ServletUtils.createServlet(slaveCommand.getLabel(), environment.metrics(), config, environment.admin(), environment.getObjectMapper());

		// inject required services
		jerseyConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(slaveCommand.getWorkers()).to(Workers.class);
			}
		});

		jerseyConfig.register(WorkerAPIResource.class);
	}

}
