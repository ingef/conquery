package com.bakdata.conquery.commands;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.bakdata.conquery.Conquery;
import com.bakdata.conquery.mode.cluster.ClusterManager;
import com.bakdata.conquery.mode.cluster.ClusterManagerProvider;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.argparse4j.inf.Namespace;
import org.jetbrains.annotations.NotNull;

@Slf4j
@Getter
public class DistributedStandaloneCommand extends io.dropwizard.cli.ServerCommand<ConqueryConfig> implements StandaloneCommand {

	private final Conquery conquery;
	private ClusterManager manager;
	private ManagerNode managerNode = new ManagerNode();

	private final List<CompletableFuture<ShardNode>> shardFutures = new ArrayList<>();

	// TODO clean up the command structure, so we can use the Environment from EnvironmentCommand
	private Environment environment;

	public DistributedStandaloneCommand(Conquery conquery) {
		super(conquery, "standalone", "starts a server and a client at the same time.");
		this.conquery = conquery;
	}

	// this must be overridden so that
	@Override
	public void run(Bootstrap<ConqueryConfig> bootstrap, Namespace namespace, ConqueryConfig configuration) throws Exception {
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

		for (int i = 0; i < configuration.getStandalone().getNumberOfShardNodes(); i++) {
			final Bootstrap<ConqueryConfig> bootstrapShard = getShardBootstrap(configuration, i);

			ShardNode sc = new ShardNode(conquery, ShardNode.DEFAULT_NAME + i);

			shardFutures.add(CompletableFuture.supplyAsync(
					() -> {
						ConqueryMDC.setLocation(sc.getName());
						try {
							sc.run(bootstrapShard, namespace);
						}
						catch (Exception e) {
							log.error("during ShardNodes creation", e);
							System.exit(-1);
						}
						return sc;
					}
			));

		}

		run(environment, namespace, configuration);
	}

	@NotNull
	private Bootstrap<ConqueryConfig> getShardBootstrap(ConqueryConfig configuration, int id) {
		final Bootstrap<ConqueryConfig> bootstrapShard = new Bootstrap<>(conquery);


		bootstrapShard.setConfigurationFactoryFactory((aClass, validator, objectMapper, s) -> new ConfigurationFactory<>() {
			@Override
			public ConqueryConfig build(ConfigurationSourceProvider configurationSourceProvider, String s) {
				return build();
			}

			@Override
			public ConqueryConfig build() {
				ConqueryConfig clone = configuration;

				if (configuration.getStorage() instanceof XodusStoreFactory) {
					final Path managerDir = ((XodusStoreFactory) configuration.getStorage()).getDirectory().resolve("shard-node" + id);
					clone = configuration
							.withStorage(((XodusStoreFactory) configuration.getStorage()).withDirectory(managerDir));

					// Define random ports for shard http endpoints
					final DefaultServerFactory factory = new DefaultServerFactory();
					HttpConnectorFactory randomPortHttpConnectorFactory = new HttpConnectorFactory();
					randomPortHttpConnectorFactory.setPort(0);
					factory.setApplicationConnectors(List.of(randomPortHttpConnectorFactory));
					factory.setAdminConnectors(List.of(randomPortHttpConnectorFactory));
					clone.setServerFactory(factory);
				}
				return clone;
			}
		});
		return bootstrapShard;
	}

	@Override
	public List<ShardNode> getShardNodes() {
		return shardFutures.stream().map(f -> {
			try {
				return f.get();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).toList();
	}

	public void run(Environment environment, Namespace namespace, ConqueryConfig config) throws Exception {
		// start ManagerNode
		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Starting ManagerNode");

		ConqueryConfig managerConfig = config;

		if (config.getStorage() instanceof XodusStoreFactory) {
			final Path managerDir = ((XodusStoreFactory) config.getStorage()).getDirectory().resolve("manager");
			managerConfig = config.withStorage(((XodusStoreFactory) config.getStorage()).withDirectory(managerDir));
		}

		manager = new ClusterManagerProvider().provideManager(managerConfig, environment);

		managerNode.run(manager);

		ConqueryMDC.setLocation("ManagerNode");
		log.debug("Starting REST Server");
		ConqueryMDC.setLocation(null);
		super.run(environment, namespace, config);
	}
}
