package com.bakdata.conquery.integration.tests;

import java.util.Set;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.util.support.StandaloneSupport;

public interface ProgrammaticIntegrationTest extends IntegrationTest {

	default boolean isEnabled(StandaloneSupport.Mode mode) {
		return forModes().contains(mode);
	}

	default Set<StandaloneSupport.Mode> forModes() {
		// Worker mode is standard until SQL connector has full feature support
		return Set.of(StandaloneSupport.Mode.WORKER);
	}

}
