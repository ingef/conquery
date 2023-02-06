package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.ToString;

/**
 * Count the number of distinct quarters of the related events. Implementation is specific for LocalDates
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CountQuartersOfDatesAggregator extends SingleColumnAggregator<Long> {

	private final IntSet quarters = new IntOpenHashSet();

	public CountQuartersOfDatesAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		quarters.clear();
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
	public Long createAggregationResult() {
		return quarters.isEmpty() ? null : (long) quarters.size();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
