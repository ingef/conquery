package com.bakdata.conquery.util.support;

import java.io.File;
import java.net.ServerSocket;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * This interface allows to override the configuration used in tests.
 *
 */
@TestInstance(Lifecycle.PER_CLASS)
public interface ConfigOverride {

	/**
	 * Is called upon initialization of the test instance of Conquery.
	 * @param config The configuration that is initialized with the defaults.
	 */
	void override(ConqueryConfig config);

	@SneakyThrows
	static void configurePathsAndLogging(ConqueryConfig config, File tmpDir) {

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
	static void configureRandomPorts(ConqueryConfig config) {

		// set random open ports
		for (ConnectorFactory con : CollectionUtils
				.union(
						((DefaultServerFactory) config.getServerFactory()).getAdminConnectors(),
						((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors()
				)) {
			try (ServerSocket s = new ServerSocket(0)) {
				((HttpConnectorFactory) con).setPort(s.getLocalPort());
			}
		}
		try (ServerSocket s = new ServerSocket(0)) {
			config.getCluster().setPort(s.getLocalPort());
		}
	}

}
