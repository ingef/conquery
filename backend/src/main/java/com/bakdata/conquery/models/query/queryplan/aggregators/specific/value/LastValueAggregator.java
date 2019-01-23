package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class LastValueAggregator extends SingleColumnAggregator<Object> {

	private Object value;
	private int date;

	private Column validityDateColumn;

	public LastValueAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		int next = block.getDate(event, validityDateColumn);

		if (next > date) {
			date = next;
			value = block.getAsObject(event, getColumn());
		}
	}

	@Override
	public Object getAggregationResult() {
		return value;
	}

	@Override
	public LastValueAggregator clone() {
		return new LastValueAggregator(getColumn());
	}
}
