package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.BeforeOrSameTemporalMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened on the same day or before the events of {@code index}. And the time where this has happened.
 */
@CPSType(id = "BEFORE_OR_SAME", base = CQElement.class)
public class CQBeforeOrSameTemporalQuery extends CQAbstractTemporalQuery {

	public CQBeforeOrSameTemporalQuery(CQSampled index, CQSampled preceding) {
		super(index, preceding);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext ctx, ConceptQueryPlan plan) {
		SpecialDateUnion dateAggregator = new SpecialDateUnion();
		plan.getDateAggregator().register(dateAggregator);

		return new TemporalQueryNode(
			index.createQueryPlan(ctx),
			preceding.createQueryPlan(ctx),
			new BeforeOrSameTemporalMatcher(),
				dateAggregator
		);
	}
}
