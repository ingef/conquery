package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;

/**
 * Aggregator, returning the min duration in the column, relative to the end of date restriction.
 */
@ToString(callSuper = true, of = "unit")
public class DateDistanceAggregator extends SingleColumnAggregator<Long> {

	private LocalDate reference;
	private ChronoUnit unit;

	private long result = Long.MAX_VALUE;
	private boolean hit;

	public DateDistanceAggregator(Column column, ChronoUnit unit) {
		super(column);
		this.unit = unit;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		hit = false;
		result = Long.MAX_VALUE;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		if(ctx.getDateRestriction().isAll() || ctx.getDateRestriction().isEmpty()){
			reference = CDate.toLocalDate(ctx.getToday());
		}
		else {
			reference = CDate.toLocalDate(ctx.getDateRestriction().getMaxValue());
		}
	}

	@Override
	public Long createAggregationResult() {
		return result != Long.MAX_VALUE || hit ? result : null;
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if(!bucket.has(event, getColumn())) {
			return;
		}

		hit = true;

		LocalDate date = CDate.toLocalDate(bucket.getDate(event, getColumn()));

		final long between = unit.between(date, reference);

		result = Math.min(result, between);
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
