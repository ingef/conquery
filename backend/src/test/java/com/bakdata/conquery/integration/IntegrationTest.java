package com.bakdata.conquery.integration;

import java.io.File;
import java.util.Map;

import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.io.Cloner;
import com.bakdata.conquery.util.io.ConqueryMDC;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.function.Executable;

public interface IntegrationTest {

	void execute(String name, TestConquery testConquery) throws Exception;

	/**
	 * Allows to adapt the configuration of the test instance that is executed for the test
	 *
	 * @param conf    A clone of the base config which can be adapted.
	 * @param workdir The workdir for the test instance
	 * @return the Config that will be used by the test instance
	 * @implNote If the config is changed a new storage path must be registered. Otherwise
	 * spinning up the instance with the new config fails because xodus is locked by the first
	 * instance. Add this code to override the storage path:
	 * <pre>
	 * // Create new storage path to prevent xodus lock conflicts
	 * XodusStoreFactory storageConfig = (XodusStoreFactory) conf.getStorage();
	 * final Path storageDir = workdir.toPath().resolve(storageConfig.getDirectory().resolve(getClass().getSimpleName()));
	 *
	 * return conf.withStorage(storageConfig.withDirectory(storageDir));
	 * </pre>
	 */
	public default ConqueryConfig overrideConfig(final ConqueryConfig conf, final File workdir) {
		return conf;
	}

	abstract class Simple implements IntegrationTest {
		public abstract void execute(StandaloneSupport conquery) throws Exception;

		@Override
		public void execute(String name, TestConquery testConquery) throws Exception {
			StandaloneSupport conquery = testConquery.getSupport(name);
			// Because Shiro works with a static Security manager
			testConquery.getStandaloneCommand().getManagerNode().getAuthController().registerStaticSecurityManager();

			try {
				execute(conquery);
			}
			finally {
				testConquery.removeSupport(conquery);
			}
		}
	}

	@Slf4j
	@RequiredArgsConstructor
	final class Wrapper implements Executable {

		private final String name;
		private final IntegrationTests integrationTests;
		private final IntegrationTest test;
		private final TestDataImporter testImporter;

		@Override
		public void execute() throws Throwable {

			ConqueryMDC.setLocation(name);
			log.info("STARTING integration test {}", name);

			// we clone the default config to ensure that nothing mangles the config of others.
			// However, override config _should_ not mutate the incoming config.

			final ConqueryConfig clonedConfig = Cloner.clone(integrationTests.getConfig(), Map.of(), IntegrationTests.MAPPER);
			final ConqueryConfig overridenConfig = test.overrideConfig(clonedConfig, integrationTests.getWorkDir());

			final TestConquery testConquery = integrationTests.getCachedConqueryInstance(integrationTests.getWorkDir(), overridenConfig, testImporter);

			try {
				testConquery.beforeEach();
				test.execute(name, testConquery);
			}
			catch (Exception e) {
				ConqueryMDC.setLocation(name);
				log.info("FAILED integration test " + name, e);
				throw e;
			}
			finally {
				testConquery.afterEach();
			}
			ConqueryMDC.setLocation(name);
			log.info("SUCCESS integration test {}", name);
		}
	}
}
