package com.bakdata.conquery.models.query.aggregators.filter.sum;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class RealSumFilterNode extends SumFilterNode<Double> {

	public RealSumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected Double combine(Double sum, Double addend) {
		return sum + addend;
	}

	@Override
	protected Double getValue(Block block, int event, Column column) {
		return block.getReal(event, column);
	}

	@Override
	public Double defaultValue() {
		return 0d;
	}

	@Override
	public RealSumFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new RealSumFilterNode(filter, filterValue);
	}
}
