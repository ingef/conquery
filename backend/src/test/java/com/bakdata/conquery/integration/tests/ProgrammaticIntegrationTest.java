package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.util.support.StandaloneSupport;

public interface ProgrammaticIntegrationTest extends IntegrationTest {

	default boolean isEnabled(StandaloneSupport.Mode mode) {
		return switch (mode) {
			case WORKER -> true;
			case SQL -> isSqlReady();
		};
	}

	/**
	 * @return True if this programmatic test can run in SQL mode.
	 */
	default boolean isSqlReady() {
		return false;
	}

}
