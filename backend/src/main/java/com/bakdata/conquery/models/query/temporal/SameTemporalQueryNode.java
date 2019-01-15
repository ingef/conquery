package com.bakdata.conquery.models.query.temporal;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.concepts.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.util.OptionalInt;

public class SameTemporalQueryNode extends AbstractTemporalQueryNode {

	public SameTemporalQueryNode(QueryPlan index, QueryPlan preceding, TemporalSampler sampler, SpecialDateUnion dateUnion) {
		super(index, preceding, sampler, dateUnion);
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new SameTemporalQueryNode(getIndex().clone(), getPreceding().clone(), getSampler(), clone.getIncluded());
	}

	@Override
	public void removePreceding(CDateSet preceding, int sample) {
		// Only consider samples that are before index's sample event
		preceding.remove(CDateRange.atLeast(sample + 1));
	}

	@Override
	public boolean isContained(OptionalInt index, OptionalInt preceding) {
		if (!preceding.isPresent() || !index.isPresent()) {
			return false;
		}

		return (index.getAsInt() == preceding.getAsInt());
	}
}
