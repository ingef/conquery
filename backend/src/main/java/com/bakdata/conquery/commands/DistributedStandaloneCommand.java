package com.bakdata.conquery.commands;

import java.nio.file.Path;
import java.util.List;
import java.util.Vector;

import com.bakdata.conquery.mode.cluster.ClusterManager;
import com.bakdata.conquery.mode.cluster.ClusterManagerProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.util.commands.NoOpConquery;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.core.cli.ServerCommand;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
@Getter
public class DistributedStandaloneCommand extends ServerCommand<ConqueryConfig> implements StandaloneCommand {

	private final ManagerNode managerNode = new ManagerNode();
	private final List<ShardNode> shardNodes = new Vector<>();
	private ClusterManager manager;

	public DistributedStandaloneCommand() {
		super(new NoOpConquery(), "standalone", "starts a manager node and shard node(s) at the same time in a single JVM.");
	}

	@Override
	protected void run(Environment environment, Namespace namespace, ConqueryConfig configuration) throws Exception {


		ConqueryConfig managerConfig = configuration;

		if (configuration.getStorage() instanceof XodusStoreFactory) {
			final Path managerDir = ((XodusStoreFactory) configuration.getStorage()).getDirectory().resolve("manager");
			managerConfig = configuration.withStorage(((XodusStoreFactory) configuration.getStorage()).withDirectory(managerDir));
		}

		manager = new ClusterManagerProvider().provideManager(managerConfig, environment);

		managerNode.run(manager);

		for (int id = 0; id < configuration.getStandalone().getNumberOfShardNodes(); id++) {

			ShardNode sc = new ShardNode(ShardNode.DEFAULT_NAME + id);

			shardNodes.add(sc);

			ConqueryMDC.setLocation(sc.getName());

			ConqueryConfig clone = configuration;

			if (configuration.getStorage() instanceof XodusStoreFactory) {
				final Path managerDir = ((XodusStoreFactory) configuration.getStorage()).getDirectory().resolve("shard-node" + id);
				clone = configuration.withStorage(((XodusStoreFactory) configuration.getStorage()).withDirectory(managerDir));
			}

			sc.run(clone, environment);
		}


		// starts the Jersey Server
		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Starting REST Server");
		ConqueryMDC.setLocation(null);
		super.run(environment, namespace, configuration);
	}
}
