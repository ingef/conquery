package com.bakdata.conquery.models.execution;

import java.util.Set;

import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.fasterxml.jackson.annotation.JsonIgnore;

public interface InternalExecution<R extends ShardResult> {


	void addResult(R result);

	WorkerMessage createExecutionMessage();

	/**
	 * Gives all {@link NamespacedId}s that were required in the execution.
	 *
	 * @return A List of all {@link NamespacedId}s needed for the execution.
	 */
	@JsonIgnore
	Set<NamespacedIdentifiable<?>> getUsedNamespacedIds();
}
