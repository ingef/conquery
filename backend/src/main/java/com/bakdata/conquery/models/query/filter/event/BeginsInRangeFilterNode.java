package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.concepts.filters.specific.DurationSumFilter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class BeginsInRangeFilterNode extends FilterNode<FilterValue.CQIntegerRangeFilter, DurationSumFilter> {

	private boolean hit;
	private CDateSet dateRestriction;

	public BeginsInRangeFilterNode(DurationSumFilter dateDistanceFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(dateDistanceFilter, filterValue);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public BeginsInRangeFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new BeginsInRangeFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		CDateRange datum = block.getDateRange(event, filter.getColumn());

		return dateRestriction.contains(datum.getMinValue());
	}

	@Override
	public void acceptEvent(Block block, int event) {
		//Base class for event based filter nodes to reduce repetition?
		this.hit = true;

	}

	@Override
	public boolean isContained() {
		return hit;
	}
}
