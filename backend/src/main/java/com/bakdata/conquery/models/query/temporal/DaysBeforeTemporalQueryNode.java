package com.bakdata.conquery.models.query.temporal;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.time.LocalDate;

public class DaysBeforeTemporalQueryNode extends AbstractTemporalQueryNode {

	private final Range.IntegerRange days;

	public DaysBeforeTemporalQueryNode(QueryPlan index, QueryPlan preceding, TemporalSampler sampler, Range.IntegerRange days, SpecialDateUnion dateUnion) {
		super(index, preceding, sampler, dateUnion);
		this.days = days;
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new DaysBeforeTemporalQueryNode(getIndex().clone(), getPreceding().clone(), getSampler(), days, clone.getIncluded());
	}

	@Override
	public boolean isContained(LocalDate index, LocalDate preceding) {
		if (preceding == null) {
			return false;
		}

		return days.contains(CDate.ofLocalDate(index) - CDate.ofLocalDate(preceding));
	}
}
