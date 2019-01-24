package com.bakdata.conquery.integration;

import com.bakdata.conquery.util.support.TestConquery;

/**
 * Generic test interface for integration tests.
 * Tests should not implement this interface directly,
 * but a child of this interface.
 *
 * @param <T> The specific test integration test instance.
 */
public interface IConqueryTestGen <T extends TestConquery> {

	/**
	 * Performs the required initialization for the test.
	 * @param conquery The test instance that provides access to all components that need to be configured.
	 */
	void init(T conquery);
	
	/**
	 * Executes the test statements
	 */
	void execute();
	
	/**
	 * Cleans up previously initializations for the next test.
	 */
	void finish();
}
