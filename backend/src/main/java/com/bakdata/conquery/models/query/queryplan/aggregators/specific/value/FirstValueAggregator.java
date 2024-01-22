package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Aggregator, returning the first value (by validity date) of a column.
 * @param <VALUE> Value type of the column/return value
 */
@Slf4j
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class FirstValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private OptionalInt selectedEvent = OptionalInt.empty();
	private Bucket selectedBucket;

	private int date = CDateRange.POSITIVE_INFINITY;

	private ValidityDate validityDate;

	public FirstValueAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		selectedEvent = OptionalInt.empty();
		date = CDateRange.POSITIVE_INFINITY;
		selectedBucket = null;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		validityDate = ctx.getValidityDateColumn();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		if (validityDate == null) {
			// If there is no validity date, take the first possible value
			if(selectedBucket == null) {
				selectedBucket = bucket;
				selectedEvent = OptionalInt.of(event);
			} else {
				log.trace("There is more than one value for the {}. Choosing the very first one encountered", getClass().getSimpleName());
			}
			return;
		}


		final CDateRange dateRange = validityDate.getValidityDate(event, bucket);

		if (dateRange == null){
			return;
		}

		final int next = dateRange.getMinValue();

		if (next < date) {
			date = next;
			selectedEvent = OptionalInt.of(event);
			selectedBucket = bucket;
		}
		else if (next == date) {
			log.trace("There is more than one value for the {}. Choosing the very first one encountered", getClass().getSimpleName());
		}
	}

	@Override
	public VALUE createAggregationResult() {
		if (selectedBucket == null && selectedEvent.isEmpty()) {
			return null;
		}

		return (VALUE) selectedBucket.createScriptValue(selectedEvent.getAsInt(), getColumn());
	}

	@Override
	public ResultType getResultType() {
		return ResultType.resolveResultType(getColumn().getType());
	}
}
