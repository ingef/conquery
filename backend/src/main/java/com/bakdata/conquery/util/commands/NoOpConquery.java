package com.bakdata.conquery.util.commands;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import io.dropwizard.core.setup.Environment;

/**
 * {@link io.dropwizard.core.Application} "placeholder" for {@link io.dropwizard.core.cli.ServerCommand}/{@link io.dropwizard.core.cli.EnvironmentCommand}s that should not
 * run {@link ManagerNode} related code.
 * <p/>
 * The {@link io.dropwizard.core.cli.EnvironmentCommand} calls normally {@link Conquery#run(ConqueryConfig, Environment)} which brings ub the manager.
 */
public class NoOpConquery extends Conquery {

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		// Do nothing
	}
}
