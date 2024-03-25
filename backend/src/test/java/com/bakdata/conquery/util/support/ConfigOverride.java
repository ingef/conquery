package com.bakdata.conquery.util.support;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Path;

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
	static ConqueryConfig defaultConfig(File workDir) {

		ConqueryConfig config = new ConqueryConfig();

		config.setFailOnError(true);

		config.getStandalone().setNumberOfShardNodes(2);

		config.getCluster().setEntityBucketSize(3);
		config.getCluster().setWaitReconnect(1);

		return config;
	}

	@SneakyThrows
	static void configureRandomPorts(ConqueryConfig config) {

		// set random open ports
		for (ConnectorFactory con : CollectionUtils
				.union(
						((DefaultServerFactory) config.getServerFactory()).getAdminConnectors(),
						((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors()
				)) {
			((HttpConnectorFactory) con).setPort(0);
		}
		try (ServerSocket s = new ServerSocket(0)) {
			config.getCluster().setPort(s.getLocalPort());
		}
	}

	static void configureStoragePath(ConqueryConfig config, Path workdir) {
		((XodusStoreFactory) config.getStorage()).setDirectory(workdir);
	}

}
