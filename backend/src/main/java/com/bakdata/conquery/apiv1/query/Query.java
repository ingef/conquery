package com.bakdata.conquery.apiv1.query;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class Query implements QueryDescription {

	public abstract QueryPlan<?> createQueryPlan(QueryPlanContext context);
	
	public abstract void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries);
	
	@Override
	public abstract void resolve(QueryResolveContext context);
	
	public Set<ManagedExecution<?>> collectRequiredQueries() {
		Set<ManagedExecution<?>> set = new HashSet<>();
		collectRequiredQueries(set);
		return set;
	}

	public ResultInfoCollector collectResultInfos() {
		ResultInfoCollector collector = new ResultInfoCollector();
		collectResultInfos(collector);
		return collector;
	}
	
	public abstract void collectResultInfos(ResultInfoCollector collector);
	
	@Override
	public ManagedQuery toManagedExecution(User user, Dataset submittedDataset) {
		return new ManagedQuery(this, user, submittedDataset);
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
	 * @see ManagedQuery#finish(MetaStorage, ExecutionState) for how it's used.
	 * @return the number of results in the result List.
	 * @param results
	 */
	public long countResults(Stream<EntityResult> results) {
		return results.count();
	}
}
