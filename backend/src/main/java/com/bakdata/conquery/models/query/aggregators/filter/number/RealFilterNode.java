package com.bakdata.conquery.models.query.aggregators.filter.number;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class RealFilterNode extends NumberFilterNode<Double> {

	public RealFilterNode(NumberFilter filter, FilterValue<Range.DoubleRange> filterValue) {
		super(filter, filterValue);
	}


	@Override
	protected Double getValue(Block block, int event, Column column) {
		return block.getReal(event, filter.getColumn());
	}

	@Override
	public RealFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new RealFilterNode(filter, (FilterValue<Range.DoubleRange>) filterValue);
	}
}
