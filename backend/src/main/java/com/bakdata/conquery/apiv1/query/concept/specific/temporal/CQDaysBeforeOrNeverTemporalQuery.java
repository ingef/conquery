package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.DaysBeforeOrNeverPrecedenceMatcher;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalQueryNode;
import lombok.Getter;

/**
 * Creates a query that will contain all entities where {@code preceding} contains events that happened {@code days} before the events of {@code index}, or no events. And the time where this has happened.
 */
@CPSType(id = "DAYS_OR_NO_EVENT_BEFORE", base = CQElement.class)
public class CQDaysBeforeOrNeverTemporalQuery extends CQAbstractTemporalQuery {

	@Getter
	private int days;

	public CQDaysBeforeOrNeverTemporalQuery(CQSampled index, CQSampled preceding, int days) {
		super(index, preceding);
		this.days = days;
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext ctx, ConceptQueryPlan plan) {
		SpecialDateUnion dateAggregator = new SpecialDateUnion();
		plan.getDateAggregator().register(dateAggregator);

		return new TemporalQueryNode(
				index.createQueryPlan(ctx),
				preceding.createQueryPlan(ctx),
				new DaysBeforeOrNeverPrecedenceMatcher(days),
				dateAggregator
		);
	}
}
