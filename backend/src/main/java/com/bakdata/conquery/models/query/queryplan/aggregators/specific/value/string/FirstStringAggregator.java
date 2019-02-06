package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.string;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.CType;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class FirstStringAggregator extends SingleColumnAggregator<String> {

	private String value;
	private int date = Integer.MAX_VALUE;

	private Column validityDateColumn;

	public FirstStringAggregator(SelectId id, Column column) {
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

		if (next < date) {
			date = next;
			value = (String) ((CType<Integer, ?>) getColumn().getTypeFor(block)).createScriptValue(block.getString(event, getColumn()));
		}
	}

	@Override
	public String getAggregationResult() {
		return value;
	}

	@Override
	public FirstStringAggregator clone() {
		return new FirstStringAggregator(getId(), getColumn());
	}
}
