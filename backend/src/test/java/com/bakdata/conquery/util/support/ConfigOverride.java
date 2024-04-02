package com.bakdata.conquery.util.support;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Path;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import lombok.SneakyThrows;
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

		config.getCluster().setEntityBucketSize(3);
		config.getCluster().setWaitReconnect(1);

		return config;
	}

	@SneakyThrows
	static void configureRandomPorts(ConqueryConfig config) {

		try (ServerSocket s0 = new ServerSocket(0); ServerSocket s1 = new ServerSocket(0); ServerSocket s2 = new ServerSocket(0)) {
			// set random open ports
			((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getAdminConnectors().get(0)).setPort(s0.getLocalPort());
			((HttpConnectorFactory) ((DefaultServerFactory) config.getServerFactory()).getApplicationConnectors().get(0)).setPort(s1.getLocalPort());
			config.getCluster().setPort(s2.getLocalPort());
		}
	}

	static void configureStoragePath(ConqueryConfig config, Path workdir) {
		if (config.getStorage() instanceof XodusStoreFactory xodusStoreFactory) {
			xodusStoreFactory.setDirectory(workdir);
		}
	}

}
