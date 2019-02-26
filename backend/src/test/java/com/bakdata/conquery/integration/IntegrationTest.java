package com.bakdata.conquery.integration;

import org.junit.jupiter.api.function.Executable;

import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;

import lombok.RequiredArgsConstructor;

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
	
	@RequiredArgsConstructor
	static final class Wrapper implements Executable {
		private final TestConquery testConquery;
		private final IntegrationTest test;
		
		@Override
		public void execute() throws Throwable {
			test.execute(testConquery);
		}
	}
}
