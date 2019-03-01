package com.bakdata.conquery.models.query.filter.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.concepts.filters.specific.DateDistanceFilter;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Entity is included as long as Dates are within a certain range.
 */
@Slf4j
public class DateDistanceFilterNode extends FilterNode<FilterValue.CQIntegerRangeFilter, DateDistanceFilter> {

	private boolean hit = false;
	private LocalDate reference;
	private ChronoUnit unit;

	public DateDistanceFilterNode(DateDistanceFilter dateDistanceFilter, FilterValue.CQIntegerRangeFilter filterValue, ChronoUnit unit) {
		super(dateDistanceFilter, filterValue);
		this.unit = unit;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		reference = CDate.toLocalDate(ctx.getDateRestriction().getMinValue());
	}

	@Override
	public DateDistanceFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new DateDistanceFilterNode(filter, filterValue, unit);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, filter.getColumn())) {
			return false;
		}

		LocalDate date = CDate.toLocalDate(block.getDate(event, filter.getColumn()));

		final long between = unit.between(date, reference);

		log.info("{} between {} - {} = {}", unit, reference, date, between);

		return filterValue.getValue().contains(between);
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
