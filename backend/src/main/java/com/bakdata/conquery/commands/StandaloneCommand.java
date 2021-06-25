package com.bakdata.conquery.commands;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;

@Slf4j
@Getter
public class StandaloneCommand extends io.dropwizard.cli.ServerCommand<ConqueryConfig> {

	private final Conquery conquery;
	private ManagerNode manager = new ManagerNode();
	private final List<ShardNode> shardNodes = new Vector<>();

	// TODO clean up the command structure, so we can use the Environment from EnvironmentCommand
	private Environment environment;

	public StandaloneCommand(Conquery conquery) {
		super(conquery, "standalone", "starts a server and a client at the same time.");
		this.conquery = conquery;
	}

	// this must be overridden so that
	@Override
	protected void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
		environment = new Environment(
				bootstrap.getApplication().getName(),
				bootstrap.getObjectMapper(),
				bootstrap.getValidatorFactory(),
				bootstrap.getMetricRegistry(),
				bootstrap.getClassLoader(),
				bootstrap.getHealthCheckRegistry(),
				configuration
		);
		configuration.getMetricsFactory().configure(environment.lifecycle(), bootstrap.getMetricRegistry());
		configuration.getServerFactory().configure(environment);

		bootstrap.run(configuration, environment);
		startStandalone(environment, namespace, configuration);
	}

	protected void startStandalone(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		// start ManagerNode
		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Starting ManagerNode");

		ConqueryConfig managerConfig = config;

		if (config.getStorage() instanceof XodusStoreFactory) {
			final Path managerDir = ((XodusStoreFactory) config.getStorage()).getDirectory().resolve("manager");
			managerConfig = config.withStorage(((XodusStoreFactory) config.getStorage()).withDirectory(managerDir));
		}


		conquery.setManager(manager);
		conquery.run(managerConfig, environment);

		//create thread pool to start multiple ShardNodes at the same time
		ExecutorService starterPool = Executors.newFixedThreadPool(
				config.getStandalone().getNumberOfShardNodes(),
				new ThreadFactoryBuilder()
						.setNameFormat("ShardNode Storage Loader %d")
						.setUncaughtExceptionHandler((t, e) -> {
							ConqueryMDC.setLocation(t.getName());
							log.error(t.getName() + " failed to init storage of ShardNode", e);
						})
						.build()
		);

		List<Future<ShardNode>> tasks = new ArrayList<>();
		for (int i = 0; i < config.getStandalone().getNumberOfShardNodes(); i++) {

			final int id = i;

			tasks.add(starterPool.submit(() -> {
				ShardNode sc = new ShardNode(ShardNode.DEFAULT_NAME + id);

				shardNodes.add(sc);

				ConqueryMDC.setLocation(sc.getName());

				ConqueryConfig clone = config;

				if (config.getStorage() instanceof XodusStoreFactory) {
					final Path managerDir = ((XodusStoreFactory) config.getStorage()).getDirectory().resolve("shard-node" + id);
					clone = config.withStorage(((XodusStoreFactory) config.getStorage()).withDirectory(managerDir));
				}

				sc.run(environment, namespace, clone);
				return sc;
			}));
		}
		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Waiting for ShardNodes to start");
		starterPool.shutdown();
		starterPool.awaitTermination(1, TimeUnit.HOURS);
		//catch exceptions on tasks
		boolean failed = false;
		for (Future<ShardNode> f : tasks) {
			try {
				f.get();
			}
			catch (ExecutionException e) {
				log.error("during ShardNodes creation", e);
				failed = true;
			}
		}
		if (failed) {
			System.exit(-1);
		}

		// starts the Jersey Server
		log.debug("Starting REST Server");
		ConqueryMDC.setLocation(null);
		super.run(environment, namespace, config);
		manager = conquery.getManager();
	}
}
