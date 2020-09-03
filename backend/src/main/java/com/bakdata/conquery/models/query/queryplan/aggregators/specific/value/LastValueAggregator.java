package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, returning the last value (by validity date) of a column.
 * @param <VALUE> Value type of the column/return value
 */
public class LastValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private Object value;
	private int date;
	private Bucket bucket;

	private Column validityDateColumn;

	public LastValueAggregator(Column column) {
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

		int next = bucket.getAsDateRange(event, validityDateColumn).getMaxValue();

		if (next > date) {
			date = next;
			value = bucket.getAsObject(event, getColumn());
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
	public LastValueAggregator doClone(CloneContext ctx) {
		return new LastValueAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.resolveResultType(getColumn().getType());
	}
}
