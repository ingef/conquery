package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends FilterNode<FilterValue.CQIntegerRangeFilter, DateDistanceFilter> {

	private boolean hit;
	private CDateRange dateRestriction;

	public DateDistanceFilterNode(DateDistanceFilter dateDistanceFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(dateDistanceFilter, filterValue);
	}


	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public FilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DateDistanceFilterNode(filter, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (dateRestriction == null) {
			return true;
		}

		if (!block.has(event, filter.getColumn())) {
			return false;
		}


		int date = block.getDate(event, filter.getColumn());

		if (date <= dateRestriction.getMinValue()) {
			return filterValue.getValue().contains(dateRestriction.getMinValue() - date);
		}
		else {
			return filterValue.getValue().contains(dateRestriction.getMaxValue() - date);
		}
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
