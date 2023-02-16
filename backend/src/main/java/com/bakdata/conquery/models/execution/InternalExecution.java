package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.results.ShardResult;

/**
 * This interface must be implemented if a {@link ManagedExecution} requires direct computation using the query engine on the shard nodes.
 *
 * @param <R> The type of result, the execution assumes to be returned from the shards.
 */
public interface InternalExecution<R extends ShardResult> {

	/**
	 * The message that is send to the shard nodes
	 */
	WorkerMessage createExecutionMessage();

	/**
	 * The callback for the results the shard nodes return.
	 * Is called once per shard node
	 */
	void addResult(R result);
}
