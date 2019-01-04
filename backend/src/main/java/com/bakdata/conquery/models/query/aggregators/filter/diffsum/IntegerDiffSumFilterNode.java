package com.bakdata.conquery.models.query.aggregators.filter.diffsum;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class IntegerDiffSumFilterNode extends DiffSumFilterNode<Long> {

	public IntegerDiffSumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected Long combine(Long sum, Long addend, Long subtrahend) {
		return sum + addend - subtrahend;
	}

	@Override
	protected Long getValue(Block block, int event, Column column) {
		return block.has(event, column) ? block.getInteger(event, column) : 0;
	}

	@Override
	public Long defaultValue() {
		return 0L;
	}

	@Override
	public IntegerDiffSumFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new IntegerDiffSumFilterNode(filter, filterValue);
	}
}
