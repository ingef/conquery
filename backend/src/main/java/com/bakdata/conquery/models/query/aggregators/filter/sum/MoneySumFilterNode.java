package com.bakdata.conquery.models.query.aggregators.filter.sum;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class MoneySumFilterNode extends SumFilterNode<Long> {

	public MoneySumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected Long combine(Long sum, Long addend) {
		return sum + addend;
	}

	@Override
	protected Long getValue(Block block, int event, Column column) {
		return block.getMoney(event, column);
	}

	@Override
	public Long defaultValue() {
		return 0L;
	}

	@Override
	public MoneySumFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new MoneySumFilterNode(filter, filterValue);
	}
}
