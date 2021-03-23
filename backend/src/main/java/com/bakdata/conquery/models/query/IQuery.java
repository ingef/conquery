package com.bakdata.conquery.models.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class IQuery implements QueryDescription {

	public abstract QueryPlan createQueryPlan(QueryPlanContext context);
	
	public abstract void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries);
	
	@Override
	public abstract void resolve(QueryResolveContext context);
	
	public Set<ManagedExecutionId> collectRequiredQueries() {
		HashSet<ManagedExecutionId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}

	public ResultInfoCollector collectResultInfos() {
		ResultInfoCollector collector = new ResultInfoCollector();
		collectResultInfos(collector);
		return collector;
	}
	
	public abstract void collectResultInfos(ResultInfoCollector collector);
	
	@Override
	public ManagedQuery toManagedExecution(DatasetRegistry namespaces, UserId userId, DatasetId submittedDataset) {
		return new ManagedQuery(this,userId, submittedDataset);
	}

	/**
	 * Method that returns only the parts of the query to reusable by others. This allows switching between different implementations of {@link IQuery} between reuse.
	 */
	@JsonIgnore
	public CQElement getReusableComponents() {
		throw new IllegalArgumentException(String.format("Query of Type[%s] cannot be reused", getClass()));
	}

	/**
	 * Implement Query-type aware counting of results. Standard method is counting unique entities.
	 *
	 * @see ManagedQuery#finish(MetaStorage, ExecutionState) for how it's used.
	 * @return the number of results in the result List.
	 */
	public long countResults(List<EntityResult> results) {
		return results.stream().flatMap(ContainedEntityResult::filterCast).count();
	}
}
