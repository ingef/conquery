package com.bakdata.conquery.models.query.concept.specific.temporal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.BeforeTemporalPrecedenceMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened before the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "BEFORE", base = CQElement.class)
public class CQBeforeTemporalQuery extends CQAbstractTemporalQuery {


	public CQBeforeTemporalQuery(CQSampled index, CQSampled preceding) {
		super(index, preceding);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext registry, ConceptQueryPlan plan) {
		return new TemporalQueryNode(index.createQueryPlan(registry, plan), preceding.createQueryPlan(registry, plan), new BeforeTemporalPrecedenceMatcher(), plan.getSpecialDateUnion());
	}
	
	@Override
	public CQBeforeTemporalQuery resolve(QueryResolveContext context) {
		return new CQBeforeTemporalQuery(index.resolve(context), preceding.resolve(context));
	}
}
