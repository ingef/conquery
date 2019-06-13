package com.bakdata.eva.query.aggregators;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Very specific aggregator used for receiving values aligned with the specific quarters in the observation period.
 * @param <VALUE>
 */
public class ValueAtBeginOfQuarterAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private Object value;
	private Block block;

	private Column validityDateColumn;
	private int firstDayOfQuarter;

	public ValueAtBeginOfQuarterAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		firstDayOfQuarter = CDate.ofLocalDate(QuarterUtils.getFirstDayOfQuarter(ctx.getDateRestriction().getMinValue()));

		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn()) || ! block.has(event, validityDateColumn)) {
			return;
		}

		int next = block.getAsDateRange(event, validityDateColumn).getMinValue();

		if (next == firstDayOfQuarter) {
			value = block.getRaw(event, getColumn());
			this.block = block;
		}
	}

	@Override
	public VALUE getAggregationResult() {
		if (block == null) {
			return null;
		}

		return (VALUE) getColumn().getTypeFor(block).createPrintValue(value);
	}

	@Override
	public ValueAtBeginOfQuarterAggregator doClone(CloneContext ctx) {
		return new ValueAtBeginOfQuarterAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.resolveResultType(getColumn().getType());
	}
}
