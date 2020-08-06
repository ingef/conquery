package com.bakdata.conquery.models.query.filter.event;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.EventFilterNode;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceFilterNode extends EventFilterNode<Range.LongRange> {

	private final ChronoUnit unit;

	private LocalDate reference;

	@NotNull
	@Getter
	@Setter
	private Column column;

	@Override
	public boolean isAlwaysActive() {
		return false;
	}

	public DateDistanceFilterNode(Column column, ChronoUnit unit, Range.LongRange filterValue) {
		super(filterValue);
		this.column = column;
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

		LocalDate date = bucket.getAsDateRange(event, getColumn()).getMin();

		final long between = unit.between(date, reference);

		return filterValue.contains(between);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(getColumn().getTable().getId());
	}
}
