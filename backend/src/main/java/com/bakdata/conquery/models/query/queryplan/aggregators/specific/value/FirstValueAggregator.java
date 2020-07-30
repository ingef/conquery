package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, returning the first value (by validity date) of a column.
 * @param <VALUE> Value type of the column/return value
 */
public class FirstValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private Object value;
	private Bucket bucket;

	private int date = Integer.MAX_VALUE;

	private Column validityDateColumn;

	public FirstValueAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn()) || ! bucket.has(event, validityDateColumn)) {
			return;
		}

		int next = bucket.getAsDateRange(event, validityDateColumn).getMinValue();

		if (next < date) {
			date = next;
			value = bucket.getRaw(event, getColumn());
			this.bucket = bucket;
		}
	}

	@Override
	public VALUE getAggregationResult() {
		if (bucket == null) {
			return null;
		}

		return (VALUE) getColumn().getTypeFor(bucket).createPrintValue(value);
	}

	@Override
	public FirstValueAggregator doClone(CloneContext ctx) {
		return new FirstValueAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.resolveResultType(getColumn().getType());
	}
}
