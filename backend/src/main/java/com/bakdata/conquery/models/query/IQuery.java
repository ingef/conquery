package com.bakdata.conquery.models.query;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface IQuery {

	QueryPlan createQueryPlan(QueryPlanContext context);
	
	void collectRequiredQueries(Set<ManagedQueryId> requiredQueries);
	
	default Set<ManagedQueryId> collectRequiredQueries() {
		HashSet<ManagedQueryId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}
}
