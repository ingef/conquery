package com.bakdata.conquery.models.query;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.MoreCollectors;
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
	 * Tries to extract the {@link DatasetId} from the submitted query.
	 * If none could be extracted the alternative dataset is chosen.
	 * When more than one {@link DatasetId} is found an {@link IllegalArgumentException} is thrown.
	 */
	private static DatasetId getDataset(IQuery query, DatasetId alternativeDataset) {
		NamespacedIdCollector collector = new QueryUtils.NamespacedIdCollector();
		query.visit(collector);
		// A query of this type is only allowed to reference a single dataset as it is executed in a single namespace of that dataset.
		Optional<DatasetId> datasetOp = collector.getIds().stream().map(NamespacedId::getDataset).distinct().collect(MoreCollectors.toOptional());
		// Some submitted queries do not have a dataset reference included (e.g. queries consisting solely of CQExternal, CQReusedQuery),
		// than the dataset is chosen under which the query was submitted
		DatasetId dataset = datasetOp.orElse(alternativeDataset);
		return dataset;
	}

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
