package com.bakdata.conquery.apiv1.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.ExecutionManager;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class Query implements QueryDescription {

	public abstract QueryPlan<?> createQueryPlan(QueryPlanContext context);

	public abstract void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries);

	@Override
	public abstract void resolve(QueryResolveContext context);

	public Set<ManagedExecutionId> collectRequiredQueries() {
		Set<ManagedExecutionId> set = new HashSet<>();
		collectRequiredQueries(set);
		return set;
	}

	@JsonIgnore
	public abstract List<ResultInfo> getResultInfos();

	@Override
	public ManagedQuery toManagedExecution(UserId user, DatasetId submittedDataset, MetaStorage storage, DatasetRegistry<?> datasetRegistry) {
		return new ManagedQuery(this, user, submittedDataset, storage, datasetRegistry);
	}

	/**
	 * Method that returns only the parts of the query to reusable by others. This allows switching between different implementations of {@link Query} between reuse.
	 */
	@JsonIgnore
	public CQElement getReusableComponents() {
		throw new IllegalArgumentException(String.format("Query of Type[%s] cannot be reused", getClass()));
	}

	/**
	 * Implement Query-type aware counting of results. Standard method is counting unique entities.
	 *
	 * @param results
	 * @return the number of results in the result List.
	 * @see ManagedExecution#finish(ExecutionState, ExecutionManager)  for how it's used.
	 */
	public long countResults(Stream<EntityResult> results) {
		return results.map(EntityResult::listResultLines)
					  .mapToLong(List::size)
					  .sum();
	}
}
