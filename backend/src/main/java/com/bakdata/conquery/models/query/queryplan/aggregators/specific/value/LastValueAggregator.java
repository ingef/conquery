package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.OptionalInt;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Aggregator, returning the last value (by validity date) of a column.
 * @param <VALUE> Value type of the column/return value
 */
@Slf4j
public class LastValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {


	private OptionalInt selectedEvent = OptionalInt.empty();
	private Bucket selectedBucket;
	private int date;

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
		if (!bucket.has(event, getColumn())) {
			return;
		}
		
		if (validityDateColumn == null) {
			// If there is no validity date, take the first possible value
			if(selectedBucket == null) {
				selectedBucket = bucket;
				selectedEvent = OptionalInt.of(event);
			} else {
				log.trace("There is more than one value for the {}. Choosing the very first one encountered", this.getClass().getSimpleName());
			}
			return;			
		}
		
		if(! bucket.has(event, validityDateColumn)) {
			// TODO this might be an IllegalState
			return;
		}


		int next = bucket.getAsDateRange(event, validityDateColumn).getMaxValue();

		if (next > date) {
			date = next;
			selectedEvent = OptionalInt.of(event);
			selectedBucket = bucket;
		}
		else if (next == date) {
			log.trace("There is more than one value for the {}. Choosing the very first one encountered", this.getClass().getSimpleName());
		}
	}

	@Override
	public VALUE getAggregationResult() {
		if (selectedBucket == null && selectedEvent.isEmpty()) {
			return null;
		}

		return (VALUE) selectedBucket.createScriptValue(selectedEvent.getAsInt(), getColumn());
	}

	@Override
	public LastValueAggregator<VALUE> doClone(CloneContext ctx) {
		return new LastValueAggregator<VALUE>(getColumn());
	}
	
	@Override
	public SimpleResultType getResultType() {
		return SimpleResultType.resolveResultType(getColumn().getType());
	}
}
