package com.bakdata.conquery.util.support;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.bakdata.conquery.models.config.ConqueryConfig;

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
}
