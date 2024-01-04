package com.bakdata.conquery.integration.json;

import com.bakdata.conquery.integration.json.filter.FilterTest;
import com.bakdata.conquery.util.support.StandaloneSupport;

public interface TestDataImporter {

	void importQueryTestData(StandaloneSupport support, QueryTest queryTest) throws Exception;

	void importFormTestData(StandaloneSupport support, FormTest formTest) throws Exception;

	void importFilterTestData(StandaloneSupport support, FilterTest filterTest) throws Exception;

	@FunctionalInterface
	interface CheckedRunnable<E extends Exception> extends Runnable {

		@Override
		default void run() throws RuntimeException {
			try {
				runThrows();
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		void runThrows() throws E;

	}

}
