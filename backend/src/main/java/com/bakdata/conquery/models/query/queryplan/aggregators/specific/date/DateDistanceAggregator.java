package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, returning the min duration in the column, relative to the end of date restriction.
 */
public class DateDistanceAggregator implements Aggregator<Long> {

	private LocalDate reference;
	private ChronoUnit unit;

	private int result = Integer.MIN_VALUE;

	private Column column;

	public DateDistanceAggregator(ChronoUnit unit) {
		this.unit = unit;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		if(ctx.getDateRestriction().isAll() || ctx.getDateRestriction().isEmpty()){
			reference = LocalDate.now();
		}
		else {
			reference = CDate.toLocalDate(ctx.getDateRestriction().getMaxValue());
		}

		column = ctx.getValidityDateColumn();
		if(!column.getType().isDateCompatible()){
			throw new IllegalStateException(String.format("Non date-compatible validityDate-Column[%s]", column));
		}
	}

	@Override
	public DateDistanceAggregator doClone(CloneContext ctx) {
		return new DateDistanceAggregator(unit);
	}

	@Override
	public Long getAggregationResult() {
		return result == Integer.MIN_VALUE ? null : unit.between(CDate.toLocalDate(result), reference);
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if(!bucket.has(event, column)) {
			return;
		}

		result = Math.max(result, bucket.getAsDateRange(event, column).getMinValue());
	}

	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
