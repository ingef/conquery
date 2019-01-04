package com.bakdata.conquery.models.query.aggregators.filter.number;

import java.math.BigDecimal;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.concepts.filters.specific.NumberFilter;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class DecimalFilterNode extends NumberFilterNode<BigDecimal> {

	public DecimalFilterNode(NumberFilter filter, FilterValue<Range<BigDecimal>> filterValue) {
		super(filter, filterValue);
	}


	@Override
	protected BigDecimal getValue(Block block, int event, Column column) {
		return block.getDecimal(event, filter.getColumn());
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new DecimalFilterNode(filter, (FilterValue<Range<BigDecimal>>) filterValue);
	}
}
