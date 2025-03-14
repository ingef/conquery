package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;

/**
 * This interface must be implemented if a {@link ManagedExecution} requires direct computation using the query engine on the shard nodes.
 *
 */
public interface InternalExecution {

	WorkerMessage createExecutionMessage();

}
