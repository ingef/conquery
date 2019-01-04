package com.bakdata.conquery.models.query.aggregators.filter.sum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class DecimalSumFilterNode extends SumFilterNode<BigDecimal> {

	public DecimalSumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected BigDecimal combine(BigDecimal sum, BigDecimal addend) {
		return sum.add(addend);
	}

	@Override
	protected BigDecimal getValue(Block block, int event, Column column) {
		return block.getDecimal(event, column);
	}

	@Override
	public BigDecimal defaultValue() {
		return BigDecimal.ZERO;
	}

	@Override
	public DecimalSumFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DecimalSumFilterNode(filter, filterValue);
	}
}
