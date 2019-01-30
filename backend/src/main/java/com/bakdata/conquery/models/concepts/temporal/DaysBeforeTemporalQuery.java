package com.bakdata.conquery.models.concepts.temporal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.temporal.DaysBeforeTemporalQueryNode;
import lombok.Getter;

@CPSType(id = "DAYS_BEFORE", base = CQElement.class)
public class DaysBeforeTemporalQuery extends AbstractTemporalQuery {

	@Getter
	private Range.IntegerRange days;

	public DaysBeforeTemporalQuery(CQElement index, CQElement preceding, TemporalSampler sampler, Range.IntegerRange days) {
		super(index, preceding, sampler);
		this.days = days;
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext registry, QueryPlan plan) {
		QueryPlan indexPlan = QueryPlan.create();
		indexPlan.setRoot(index.createQueryPlan(registry, plan));

		QueryPlan precedingPlan = QueryPlan.create();
		precedingPlan.setRoot(preceding.createQueryPlan(registry, plan));

		return new DaysBeforeTemporalQueryNode(indexPlan, precedingPlan, getSampler(), days, plan.getIncluded());
	}
}
