package com.bakdata.conquery.models.query.concept;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface CQElement extends Visitable{

	default CQElement resolve(QueryResolveContext context) {
		return this;
	}

	QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan);

	default void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {}
	
	
	default Set<ManagedExecutionId> collectRequiredQueries() {
		HashSet<ManagedExecutionId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}

	default ResultInfoCollector collectResultInfos(PrintSettings config) {
		ResultInfoCollector collector = new ResultInfoCollector(config);
		collectResultInfos(collector);
		return collector;
	}
	
	void collectResultInfos(ResultInfoCollector collector);

	default void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
	}
}
