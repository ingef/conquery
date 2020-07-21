package com.bakdata.conquery.models.query.filter.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnEventFilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends SingleColumnEventFilterNode<Range.LongRange> {

	private LocalDate reference;
	private ChronoUnit unit;

	public DateDistanceFilterNode(Column column, ChronoUnit unit, Range.LongRange filterValue) {
		super(column, filterValue);
		this.unit = unit;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		if(ctx.getDateRestriction().isAll() || ctx.getDateRestriction().isEmpty()){
			reference = null;
		}
		else {
			reference = CDate.toLocalDate(ctx.getDateRestriction().getMaxValue());
		}
	}

	@Override
	public DateDistanceFilterNode doClone(CloneContext ctx) {
		return new DateDistanceFilterNode(getColumn(), unit, filterValue);
	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}

		if (reference == null) {
			return false;
		}

		LocalDate date = CDate.toLocalDate(bucket.getDate(event, getColumn()));

		final long between = unit.between(date, reference);

		return filterValue.contains(between);
	}
}
