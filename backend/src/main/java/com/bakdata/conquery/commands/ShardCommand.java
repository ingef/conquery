package com.bakdata.conquery.commands;

import java.util.Collections;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.commands.NoOpConquery;
import io.dropwizard.core.cli.ServerCommand;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Command to run conquery as a shard node.
 */
public class ShardCommand extends ServerCommand<ConqueryConfig> {

	public ShardCommand() {
		super(new NoOpConquery(), "shard", "Connects this instance as a ShardNode to a running ManagerNode.");
	}

	@Override
	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
		bootstrap.addBundle(new ShardNode());

		super.run(bootstrap, namespace, configuration);
	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception {
		/*
		 Clear application connectors for a shard, before building the server,
		 as we only expose the metrics through the admin connector.
		 */
		((DefaultServerFactory)configuration.getServerFactory()).setApplicationConnectors(Collections.emptyList());

		super.run(environment, namespace, configuration);
	}
}
