package com.bakdata.conquery.models.query.aggregators.filter.diffsum;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class RealDiffSumFilterNode extends DiffSumFilterNode<Double> {

	public RealDiffSumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected Double combine(Double sum, Double addend, Double subtrahend) {
		return sum + addend - subtrahend;
	}

	@Override
	protected Double getValue(Block block, int event, Column column) {
		return block.has(event, column) ? block.getReal(event, column) : 0;
	}

	@Override
	public Double defaultValue() {
		return 0d;
	}

	@Override
	public RealDiffSumFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new RealDiffSumFilterNode(filter, filterValue);
	}
}
