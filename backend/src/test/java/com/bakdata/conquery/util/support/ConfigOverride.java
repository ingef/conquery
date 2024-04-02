package com.bakdata.conquery.util.support;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import lombok.SneakyThrows;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * This interface allows to override the configuration used in tests.
 */
@TestInstance(Lifecycle.PER_CLASS)
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

		return config;
	}

	@SneakyThrows
	static void configureRandomPorts(ConqueryConfig config) {

		try (
				ClosableSocketSupplier sockets = new ClosableSocketSupplier()
		) {
			// set random open ports
			((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getAdminConnectors().get(0)).setPort(sockets.get().getLocalPort());
			((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors().get(0)).setPort(sockets.get().getLocalPort());
			config.getCluster().setPort(sockets.get().getLocalPort());

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

	/**
	 * Small helper to find open ports by opening random ports together in one context.
	 * A previous implementation opened and closed ports individually which could cause a port binding collision
	 * much easier.
	 */
	static class ClosableSocketSupplier implements Supplier<ServerSocket>, AutoCloseable {

		private final List<ServerSocket> openSockets = new ArrayList<>();

		@Override
		public void close() {
			openSockets.forEach((s) -> {
				try {
					s.close();
				}
				catch (IOException e) {
					throw new IllegalStateException(e);
				}
			});
			openSockets.clear();
		}

		@Override
		@SneakyThrows
		public ServerSocket get() {
			final ServerSocket serverSocket = new ServerSocket(0);
			openSockets.add(serverSocket);
			return serverSocket;
		}
	}

}
