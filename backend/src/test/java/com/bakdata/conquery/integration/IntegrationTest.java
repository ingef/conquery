package com.bakdata.conquery.integration;

import org.junit.jupiter.api.function.Executable;

import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

public interface IntegrationTest {

	void execute(TestConquery testConquery) throws Exception;

	static interface Simple extends IntegrationTest {
		void execute(StandaloneSupport conquery) throws Exception;
		
		@Override
		default void execute(TestConquery testConquery) throws Exception {
			try(StandaloneSupport conquery = testConquery.getSupport()) {
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
				test.execute(testConquery);
			}
			catch(Exception e) {
				log.info("FAILED integration test "+name, e);
				throw e;
			}
			log.info("SUCCESS integration test {}", name);
		}
	}
}
