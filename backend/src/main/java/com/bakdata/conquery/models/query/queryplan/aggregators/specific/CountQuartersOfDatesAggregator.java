package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Count the number of distinct quarters of the related events. Implementation is specific for LocalDates
 */
public class CountQuartersOfDatesAggregator extends SingleColumnAggregator<Long> {

	private final IntSet quarters = new IntOpenHashSet();

	public CountQuartersOfDatesAggregator(Column column) {
		super(column);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		LocalDate date = CDate.toLocalDate(bucket.getDate(event, getColumn()));
		quarters.add(date.getYear() * 4 + date.get(IsoFields.QUARTER_OF_YEAR));
	}

	@Override
	public Long getAggregationResult() {
		return (long) quarters.size();
	}

	@Override
	public CountQuartersOfDatesAggregator doClone(CloneContext ctx) {
		return new CountQuartersOfDatesAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
