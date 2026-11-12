package com.bakdata.conquery.integration.tests;

import java.util.Set;
import javax.annotation.Nullable;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.util.support.StandaloneSupport;

public interface ProgrammaticIntegrationTest extends IntegrationTest {

	default boolean isEnabled(StandaloneSupport.Mode mode, @Nullable Dialect dialect) {
		return forModes().contains(mode) && (dialect == null || forDialects().contains(dialect));
	}

	default Set<StandaloneSupport.Mode> forModes() {
		// Worker mode is standard until SQL connector has full feature support
		return Set.of(StandaloneSupport.Mode.WORKER);
	}

	default Set<Dialect> forDialects() {
		return Set.of(Dialect.values());
	}

}
