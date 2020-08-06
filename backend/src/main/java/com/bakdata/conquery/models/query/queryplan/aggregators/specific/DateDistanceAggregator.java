package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, returning the min duration in the column, relative to the end of date restriction.
 */
public class DateDistanceAggregator extends SingleColumnAggregator<Long> {

	private LocalDate reference;
	private ChronoUnit unit;

	private int result = Integer.MIN_VALUE;

	public DateDistanceAggregator(Column column, ChronoUnit unit) {
		super(column);
		this.unit = unit;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		if (ctx.getDateRestriction().isAll() || ctx.getDateRestriction().isEmpty()) {
			reference = LocalDate.now();
		}
		else {
			reference = CDate.toLocalDate(ctx.getDateRestriction().getMaxValue());
		}
	}

	@Override
	public DateDistanceAggregator doClone(CloneContext ctx) {
		return new DateDistanceAggregator(getColumn(), unit);
	}

	@Override
	public Long getAggregationResult() {
		return result == Integer.MIN_VALUE ? null : unit.between(CDate.toLocalDate(result), reference);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, column)) {
			return;
		}

		result = Math.max(result, bucket.getAsDateRange(event, column).getMinValue());
	}

	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
