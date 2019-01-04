package com.bakdata.conquery.models.query.aggregators.filter.diffsum;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.IRange;
import com.bakdata.conquery.models.concepts.filters.specific.SumFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class DecimalDiffSumFilterNode extends DiffSumFilterNode<BigDecimal> {

	public DecimalDiffSumFilterNode(SumFilter filter, FilterValue<? extends IRange<?, ?>> filterValue) {
		super(filter, filterValue);
	}

	@Override
	protected BigDecimal combine(BigDecimal sum, BigDecimal addend, BigDecimal subtrahend) {
		return sum.add(addend).subtract(subtrahend);
	}

	@Override
	protected BigDecimal getValue(Block block, int event, Column column) {
		return block.has(event, column) ? block.getDecimal(event, column) : BigDecimal.ZERO;
	}

	@Override
	public BigDecimal defaultValue() {
		return BigDecimal.ZERO;
	}

	@Override
	public DecimalDiffSumFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DecimalDiffSumFilterNode(filter, filterValue);
	}
}
