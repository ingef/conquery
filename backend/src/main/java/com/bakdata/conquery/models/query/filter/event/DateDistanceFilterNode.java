package com.bakdata.conquery.models.query.filter.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends SingleColumnFilterNode<Range.LongRange> {

	private boolean hit = false;
	private LocalDate reference;
	private ChronoUnit unit;

	public DateDistanceFilterNode(Column column, ChronoUnit unit, Range.LongRange filterValue) {
		super(column, filterValue);
		this.unit = unit;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		reference = CDate.toLocalDate(ctx.getDateRestriction().getMinValue());
	}

	@Override
	public DateDistanceFilterNode doClone(CloneContext ctx) {
		return new DateDistanceFilterNode(getColumn(), unit, filterValue);
	}

	@Override
	public boolean checkEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return false;
		}

		LocalDate date = CDate.toLocalDate(block.getDate(event, getColumn()));

		final long between = unit.between(date, reference);

		return filterValue.contains(between);
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
