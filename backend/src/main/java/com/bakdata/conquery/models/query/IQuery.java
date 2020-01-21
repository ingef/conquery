package com.bakdata.conquery.models.query;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface IQuery extends Visitable{

	IQuery resolve(QueryResolveContext context);
	QueryPlan createQueryPlan(QueryPlanContext context);
	
	void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries);
	
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

}
