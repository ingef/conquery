package com.bakdata.conquery.models.query.temporal;

import com.bakdata.conquery.models.concepts.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.time.LocalDate;

public class BeforeTemporalQueryNode extends AbstractTemporalQueryNode {

	public BeforeTemporalQueryNode(QueryPlan index, QueryPlan preceding, TemporalSampler sampler, SpecialDateUnion dateUnion) {
		super(index, preceding, sampler, dateUnion);
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new BeforeTemporalQueryNode(getIndex().clone(), getPreceding().clone(), getSampler(), clone.getIncluded());
	}

	@Override
	public boolean isContained(LocalDate index, LocalDate preceding) {
		if (preceding == null) {
			return false;
		}

		return index.compareTo(preceding) > 0;
	}
}
