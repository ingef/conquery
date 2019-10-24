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
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.visitor.QueryVisitor;

import lombok.Getter;
import lombok.Setter;

/**
 * Query type that combines a set of {@link ConceptQuery}s which are separately evaluated
 * and whose results are merged. If a SpecialDateUnion is required, the result will hold 
 * the union of all dates from the separate queries.
 */
@Getter
@Setter
@CPSType(id = "ARRAY_CONCEPT_QUERY", base = IQuery.class)
public class ArrayConceptQuery implements IQuery {
	List<ConceptQuery> childQueries = new ArrayList<>();

	@Override
	public ArrayConceptQuery resolve(QueryResolveContext context) {
		for(ConceptQuery child : childQueries) {
			child = child.resolve(context);
		}
		return this;
	}

	@Override
	public ArrayConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		// Make sure the constructor and the adding is called with the same context.
		ArrayConceptQueryPlan aq = new ArrayConceptQueryPlan(context);
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
