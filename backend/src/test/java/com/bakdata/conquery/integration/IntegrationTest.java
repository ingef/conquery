package com.bakdata.conquery.integration;

import java.io.File;
import java.util.Map;

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

	public default ConqueryConfig overrideConfig(final ConqueryConfig conf, final File workdir) {
		return conf;
	}

	abstract class Simple implements IntegrationTest {
		public abstract void execute(StandaloneSupport conquery) throws Exception;

		@Override
		public void execute(String name, TestConquery testConquery) throws Exception {
			StandaloneSupport conquery = testConquery.getSupport(name);
			// Because Shiro works with a static Security manager
			testConquery.getStandaloneCommand().getManager().getAuthController().registerStaticSecurityManager();

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

		@Override
		public void execute() throws Throwable {

			ConqueryMDC.setLocation(name);
			log.info("STARTING integration test {}", name);

			// we clone the default config to ensure that nothing mangles the config of others.
			// However, override config _should_ not mutate the incoming config.

			final ConqueryConfig clonedConfig = Cloner.clone(IntegrationTests.DEFAULT_CONFIG, Map.of(), IntegrationTests.MAPPER);
			final ConqueryConfig overridenConfig = test.overrideConfig(clonedConfig, integrationTests.getWorkDir());

			final TestConquery testConquery = integrationTests.getCachedConqueryInstance(integrationTests.getWorkDir(), overridenConfig);

			try {
				testConquery.beforeEach();
				test.execute(name, testConquery);
			}
			catch (Exception e) {
				ConqueryMDC.setLocation(name);
				log.warn("FAILED integration test " + name, e);
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
