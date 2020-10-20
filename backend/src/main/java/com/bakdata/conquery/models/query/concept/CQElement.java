package com.bakdata.conquery.models.query.concept;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public abstract class CQElement implements Visitable {

	public CQElement resolve(QueryResolveContext context) {
		return this;
	}

	public abstract QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan);

	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {}
	
	
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

	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
