package com.bakdata.conquery.util.support;

import java.io.File;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import lombok.SneakyThrows;

/**
 * This interface allows to override the configuration used in tests.
 */
public interface ConfigOverride {

	/**
	 * Is called upon initialization of the test instance of Conquery.
	 *
	 * @param config The configuration that is initialized with the defaults.
	 */
	void override(ConqueryConfig config);

	@SneakyThrows
	static ConqueryConfig defaultConfig(File workDir) {

		ConqueryConfig config = new ConqueryConfig();

		config.setFailOnError(true);

		config.getCluster().setEntityBucketSize(3);
		config.getCluster().setWaitReconnect(1);

		config.setLoggingFactory(new TestLoggingFactory());

		return config;
	}

	@SneakyThrows
	static void configureRandomPorts(ConqueryConfig config) {

		try (ClosableSocketSupplier sockets = new ClosableSocketSupplier()) {
			// Manager
			((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getAdminConnectors().get(0)).setPort(sockets.get().getLocalPort());
			((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors().get(0)).setPort(sockets.get().getLocalPort());
			config.getCluster().setPort(sockets.get().getLocalPort());

			// Shards
			config.getStandalone().getShards().stream()
				  .flatMap(shard -> Stream.concat(
						  shard.getAdminConnectors().stream(),
						  shard.getApplicationConnectors().stream()
				  )).forEach(c -> ((HttpConnectorFactory) c).setPort(sockets.get().getLocalPort()));
		}
	}

	static void configureStoragePath(ConqueryConfig config, Path workdir) {
		if (config.getStorage() instanceof XodusStoreFactory xodusStoreFactory) {
			xodusStoreFactory.setDirectory(workdir);
		}
	}

}
