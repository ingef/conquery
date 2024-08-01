package com.bakdata.conquery.util.support;

import java.io.File;
import java.net.ServerSocket;
import java.util.Collection;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.collections4.CollectionUtils;

/**
 * This interface allows to override the configuration used in tests.
 */
@UtilityClass
public final class ConfigOverride {

	@SneakyThrows
	public static void configurePathsAndLogging(ConqueryConfig config, File tmpDir) {

		config.setFailOnError(true);

		XodusStoreFactory storageConfig = new XodusStoreFactory();
		storageConfig.setDirectory(tmpDir.toPath());
		config.setStorage(storageConfig);
		config.getStandalone().setNumberOfShardNodes(2);
		// configure logging
		config.setLoggingFactory(new TestLoggingFactory());

		config.getCluster().setEntityBucketSize(3);

	}

	@SneakyThrows
	public static void configureRandomPorts(ConqueryConfig config) {

		// set random open ports
		final Collection<ConnectorFactory> connectorFactories = CollectionUtils.union(
				((DefaultServerFactory) config.getServerFactory()).getAdminConnectors(),
				((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors()
		);

		for (ConnectorFactory con : connectorFactories) {
			try (ServerSocket s = new ServerSocket(0)) {
				((HttpConnectorFactory) con).setPort(s.getLocalPort());
			}
		}
		try (ServerSocket s = new ServerSocket(0)) {
			config.getCluster().setPort(s.getLocalPort());
		}
	}
}
