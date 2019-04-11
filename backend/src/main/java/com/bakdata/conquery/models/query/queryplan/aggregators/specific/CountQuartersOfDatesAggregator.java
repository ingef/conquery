package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * Entity is included when the number of distinct quarters of the related events
 * is within a given range. Implementation is specific for LocalDates
 */
public class CountQuartersOfDatesAggregator extends SingleColumnAggregator<Long> {

	private final IntSet quarters = new IntOpenHashSet();

	public CountQuartersOfDatesAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		LocalDate date = CDate.toLocalDate(block.getDate(event, getColumn()));
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
