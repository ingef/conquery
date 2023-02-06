package com.bakdata.conquery.models.execution;

public enum ExecutionState {
	/**
	 * Query has no results. This might be due to it not being executed (after a restart or from plain creation), or it was cancelled.
	 */
	NEW,
	/**
	 * Query has been submitted and is still awaiting results.
	 */
	RUNNING,
	/**
	 * Query execution failed at some point (on shards, or while resolving)
	 */
	FAILED,
	/**
	 * Query is done and has valid results.
	 */
	DONE;
}
