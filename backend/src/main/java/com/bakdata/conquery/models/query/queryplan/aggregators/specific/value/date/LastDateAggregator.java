package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.time.LocalDate;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class LastDateAggregator extends SingleColumnAggregator<LocalDate> {

	private int value;
	private int date;

	private Column validityDateColumn;

	public LastDateAggregator(SelectId id, Column column) {
		super(id, column);
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
			value = block.getDate(event, getColumn());
		}
	}

	@Override
	public LocalDate getAggregationResult() {
		return CDate.toLocalDate(value);
	}

	@Override
	public LastDateAggregator clone() {
		return new LastDateAggregator(getId(), getColumn());
	}
}
