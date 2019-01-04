package com.bakdata.conquery.models.query.concept;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.io.cps.CPSBase;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedQueryId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
@JsonTypeInfo(use=JsonTypeInfo.Id.CUSTOM, property="type")
@CPSBase
public interface CQElement {

	default CQElement resolve(QueryResolveContext context) {
		return this;
	}
	
	QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan);

	default void collectRequiredQueries(Set<ManagedQueryId> requiredQueries) {}
	
	default Set<ManagedQueryId> collectRequiredQueries() {
		HashSet<ManagedQueryId> set = new HashSet<>();
		this.collectRequiredQueries(set);
		return set;
	}
}
