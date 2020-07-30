package com.bakdata.conquery.models.query.filter.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends SingleColumnFilterNode<Range.LongRange> {

	private final ChronoUnit unit;

	private boolean hit = false;
	private LocalDate reference;

	public DateDistanceFilterNode(ChronoUnit unit, Range.LongRange filterValue, Column column) {
		super(column, filterValue);
		this.unit = unit;

		if(!column.getType().isDateCompatible()){
			throw new IllegalArgumentException(String.format("DateDistanceFilterNode requires Column to be Date compatible, but is %s", column));
		}
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		if(ctx.getDateRestriction().isAll() || ctx.getDateRestriction().isEmpty()){
			reference = null;
		}
		else {
			reference = CDate.toLocalDate(ctx.getDateRestriction().getMaxValue());
		}
	}

	@Override
	public DateDistanceFilterNode doClone(CloneContext ctx) {
		return new DateDistanceFilterNode(unit, filterValue, getColumn());
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {

	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}

		if (reference == null) {
			return false;
		}

		LocalDate date = bucket.getAsDateRange(event, getColumn()).getMin();

		final long between = unit.between(date, reference);

		return filterValue.contains(between);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		//Base class for event based filter nodes to reduce repetition?
		this.hit = true;

	}

	@Override
	public boolean isContained() {
		return hit;
	}
}
