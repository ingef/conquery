package com.bakdata.conquery.models.query.filter.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends FilterNode<Range.LongRange> {

	private final ChronoUnit unit;

	private boolean hit = false;
	private LocalDate reference;
	private Column column;

	public DateDistanceFilterNode(ChronoUnit unit, Range.LongRange filterValue) {
		super(filterValue);
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

		this.column = ctx.getValidityDateColumn();
	}

	@Override
	public DateDistanceFilterNode doClone(CloneContext ctx) {
		return new DateDistanceFilterNode(unit, filterValue);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {

	}

	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, column)) {
			return false;
		}

		if (reference == null) {
			return false;
		}

		LocalDate date = bucket.getAsDateRange(event, column).getMin();

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
