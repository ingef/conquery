package com.bakdata.conquery.models.query.concept;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.worker.Namespaces;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "CONCEPT_QUERY", base = IQuery.class)
@AllArgsConstructor(onConstructor = @__({@JsonCreator}))
public class ConceptQuery implements IQuery, Visitable {

	@Valid
	@NotNull
	protected CQElement root;

	@Override
	public ConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		ConceptQueryPlan qp = new ConceptQueryPlan(context);
		qp.setChild(root.createQueryPlan(context, qp));
		return qp;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public ConceptQuery resolve(QueryResolveContext context) {
		this.root = root.resolve(context);
		return this;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add(ConqueryConstants.DATES_INFO);
		root.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		root.visit(visitor);
	}
	
	@Override
	public ManagedExecution toManagedExecution(MasterMetaStorage storage, Namespaces namespaces, UserId userId, DatasetId submittedDataset) {
		ConceptQuery query = this.resolve(new QueryResolveContext(
			storage,
			namespaces.get(submittedDataset)
			));
		ManagedQuery managed = new ManagedQuery(storage,query,userId, submittedDataset); //TODO
		managed.initExecutable(storage, namespaces, submittedDataset);
		return managed;
	}
}