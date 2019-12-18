package com.bakdata.conquery.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.function.Executable;

public interface IntegrationTest {

	void execute(String name, TestConquery testConquery) throws Exception;

	static abstract class Simple implements IntegrationTest {
		public abstract void execute(StandaloneSupport conquery) throws Exception;
		
		@Override
		public void execute(String name, TestConquery testConquery) throws Exception {
			try(StandaloneSupport conquery = testConquery.getSupport(name)) {
				assertThat(conquery.getStandaloneCommand().getMaster().getStorage().getEnvironment().isOpen()).isTrue();
				execute(conquery);
			}
		}
	}
	
	@Slf4j
	@RequiredArgsConstructor
	static final class Wrapper implements Executable {
		private final String name;
		private final TestConquery testConquery;
		private final IntegrationTest test;
		
		@Override
		public void execute() throws Throwable {
			log.info("STARTING integration test {}", name);
			try {
				test.execute(name, testConquery);
			}
			catch(Exception e) {
				log.info("FAILED integration test "+name, e);
				throw e;
			}
			log.info("SUCCESS integration test {}", name);
		}
	}
}
