package com.bakdata.conquery.models.query;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.apiv1.SubmittedQuery;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.util.QueryUtils;
import com.bakdata.conquery.util.QueryUtils.NamespacedIdCollector;
import com.google.common.collect.MoreCollectors;

public abstract class IQuery implements SubmittedQuery{

	public abstract IQuery resolve(QueryResolveContext context);
	public abstract QueryPlan createQueryPlan(QueryPlanContext context);
	
	public abstract void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries);
	
	public Set<ManagedExecutionId> collectRequiredQueries() {
		HashSet<ManagedExecutionId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}

	public ResultInfoCollector collectResultInfos(PrintSettings config) {
		ResultInfoCollector collector = new ResultInfoCollector(config);
		collectResultInfos(collector);
		return collector;
	}
	
	public abstract void collectResultInfos(ResultInfoCollector collector);
	
	@Override
	public ManagedQuery toManagedExecution(MasterMetaStorage storage, Namespaces namespaces, UserId userId, DatasetId submittedDataset) {
		DatasetId dataset = IQuery.getDataset(this, submittedDataset);
		IQuery query = this.resolve(new QueryResolveContext(
			storage,
			namespaces.get(dataset)
			));
		ManagedQuery managed = new ManagedQuery(storage, query,userId, dataset);
		managed.initExecutable(namespaces);
		return managed;
	}

	/**
	 * Tries to extract the {@link DatasetId} from the submitted query.
	 * If none could be extracted the alternative dataset is chosen.
	 * When more than one {@link DatasetId} is found an {@link IllegalArgumentException} is thrown.
	 */
	static DatasetId getDataset(IQuery query, DatasetId alternativeDataset) {
		NamespacedIdCollector collector = new QueryUtils.NamespacedIdCollector();
		query.visit(collector);
		// A query of this type is only allowed to reference a single dataset as it is executed in a single namespace of that dataset.
		Optional<DatasetId> datasetOp = collector.getIds().stream().map(NamespacedId::getDataset).distinct().collect(MoreCollectors.toOptional());
		// Some submitted queries do not have a dataset reference included (e.g. queries consisting solely of CQExternal, CQReusedQuery),
		// than the dataset is chosen under which the query was submitted
		DatasetId dataset = datasetOp.orElse(alternativeDataset);
		return dataset;
	}

}
