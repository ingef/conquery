package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.OptionalInt;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Aggregator, returning the first value (by validity date) of a column.
 * @param <VALUE> Value type of the column/return value
 */
@Slf4j
public class FirstValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private OptionalInt selectedEvent = OptionalInt.empty();
	private Bucket selectedBucket;

	private int date = Integer.MAX_VALUE;

	private Column validityDateColumn;
	private boolean possiblyMultipleHits = false;

	public FirstValueAggregator(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}
		
		if (validityDateColumn == null) {
			// If there is no validity date, take the first possible value
			if(selectedBucket == null) {
				selectedBucket = bucket;
				selectedEvent = OptionalInt.of(event);
			} else {
				possiblyMultipleHits = true;
			}
			return;			
		}
		
		if(! bucket.has(event, validityDateColumn)) {
			// TODO this might be an IllegalState
			return;
		}

		int next = bucket.getAsDateRange(event, validityDateColumn).getMinValue();

		if (next < date) {
			date = next;
			selectedEvent = OptionalInt.of(event);
			selectedBucket = bucket;
			possiblyMultipleHits = false;
		}
		else if (next == date) {
			possiblyMultipleHits = true;
		}
	}

	@Override
	public VALUE getAggregationResult() {
		if (selectedBucket == null && selectedEvent.isEmpty()) {
			return null;
		}
		if (possiblyMultipleHits) {
			log.trace("There is more than one value for the {}. Choosing the very first one encountered", this.getClass().getSimpleName());
		}
		return (VALUE) getColumn().getTypeFor(selectedBucket).createPrintValue(selectedBucket.getRaw(selectedEvent.getAsInt(), getColumn()));
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
