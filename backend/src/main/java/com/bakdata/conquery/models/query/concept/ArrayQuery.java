package com.bakdata.conquery.models.query.concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.queryplan.ArrayQueryPlan;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@CPSType(id = "ARRAY_QUERY", base = IQuery.class)
public class ArrayQuery implements IQuery {
	List<ConceptQuery> childQueries = new ArrayList<>();

	@Override
	public ArrayQuery resolve(QueryResolveContext context) {
		for(ConceptQuery child : childQueries) {
			child = child.resolve(context);
		}
		return this;
	}

	@Override
	public ArrayQueryPlan createQueryPlan(QueryPlanContext context) {
		ArrayQueryPlan aq = new ArrayQueryPlan(context);
		aq.addChildPlans(childQueries, context);
		return aq;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		childQueries.forEach(q -> q.collectRequiredQueries(requiredQueries));
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		childQueries.forEach(q -> q.collectResultInfos(collector));
		// Remove DateInfo from each childQuery
		collector.getInfos().removeAll(List.of(ConqueryConstants.DATES_INFO));
		// Add one DateInfo for the whole Query
		collector.getInfos().add(0, ConqueryConstants.DATES_INFO);
	}

	@Override
	public void visit(QueryVisitor visitor) {
		childQueries.forEach(q -> q.visit(visitor));
	}

}
