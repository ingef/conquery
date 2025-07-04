package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import static com.bakdata.conquery.models.query.StringUtils.getSubstringFromRange;

import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Aggregator, returning the last value (by validity date) of a column.
 * @param <VALUE> Value type of the column/return value
 */
@Slf4j
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class LastValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private final Range.IntegerRange substring;

	private int selectedEvent = -1;
	private Bucket selectedBucket;
	private int date;

	private ValidityDate validityDateColumn;

	public LastValueAggregator(Column column, Range.IntegerRange substring) {
		super(column);
		this.substring = substring;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		selectedEvent = -1;
		date = CDateRange.NEGATIVE_INFINITY;
		selectedBucket = null;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}
		
		if (validityDateColumn == null) {
			// If there is no validity date, take the first possible value
			if(selectedBucket == null) {
				selectedBucket = bucket;
				selectedEvent = event;
			} else {
				log.trace("There is more than one value for the {}. Choosing the very first one encountered", this.getClass().getSimpleName());
			}
			return;			
		}

		final CDateRange dateRange = validityDateColumn.getValidityDate(event, bucket);

		if (dateRange == null){
			return;
		}

		int next = dateRange.getMaxValue();

		if (next > date) {
			date = next;
			selectedEvent = event;
			selectedBucket = bucket;
		}
		else if (next == date) {
			log.trace("There is more than one value for the {}. Choosing the very first one encountered", this.getClass().getSimpleName());
		}
	}

	@Override
	public VALUE createAggregationResult() {
		if (selectedBucket == null) {
			return null;
		}

		if (substring != null) {
			String string = selectedBucket.getString(selectedEvent, getColumn());
			return (VALUE) getSubstringFromRange(string, substring);
		}

		return (VALUE) selectedBucket.createScriptValue(selectedEvent, getColumn());
	}

}
