package com.bakdata.conquery.models.execution;

import java.util.Set;

import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.Namespace;
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


	/**
	 * Returns the set of namespaces, this execution needs to be executed on.
	 * The {@link ExecutionManager} then submits the queries to these namespaces.
	 */
	@JsonIgnore
	Set<Namespace> getRequiredDatasets();
}
