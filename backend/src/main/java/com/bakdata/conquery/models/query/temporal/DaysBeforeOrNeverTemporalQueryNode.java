package com.bakdata.conquery.models.query.temporal;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.concepts.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.time.LocalDate;

public class DaysBeforeOrNeverTemporalQueryNode extends AbstractTemporalQueryNode {

	final int days;

	public DaysBeforeOrNeverTemporalQueryNode(QueryPlan index, QueryPlan preceding, TemporalSampler sampler, int days, SpecialDateUnion dateUnion) {
		super(index, preceding, sampler, dateUnion);
		this.days = days;
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new DaysBeforeOrNeverTemporalQueryNode(getIndex().clone(), getPreceding().clone(), getSampler(), days, clone.getIncluded());
	}

	@Override
	public boolean isContained(LocalDate index, LocalDate preceding) {
		if (preceding == null) {
			return true;
		}

		return CDate.ofLocalDate(index) - CDate.ofLocalDate(preceding) >= days;
	}
}
