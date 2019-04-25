package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Entity is included as long as Dates are within a certain range.
 */
public class DateDistanceAggregatorNode extends SingleColumnAggregator<Long> {

	private LocalDate reference;
	private ChronoUnit unit;

	private long result = Long.MAX_VALUE;

	public DateDistanceAggregatorNode(Column column, ChronoUnit unit) {
		super(column);
		this.unit = unit;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		reference = CDate.toLocalDate(ctx.getDateRestriction().getMinValue());
	}

	@Override
	public DateDistanceAggregatorNode doClone(CloneContext ctx) {
		return new DateDistanceAggregatorNode(getColumn(), unit);
	}

	@Override
	public Long getAggregationResult() {
		return result == Long.MAX_VALUE ? null : result;
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		LocalDate date = CDate.toLocalDate(block.getDate(event, getColumn()));

		final long between = unit.between(date, reference);

		result = Math.min(result, between);
	}

	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
