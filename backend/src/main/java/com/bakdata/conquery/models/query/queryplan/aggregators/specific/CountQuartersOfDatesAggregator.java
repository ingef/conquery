package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

/**
 * Entity is included when the number of distinct quarters of the related events
 * is within a given range. Implementation is specific for LocalDates
 */
public class CountQuartersOfDatesAggregator extends SingleColumnAggregator<Long> {

	private final IntSet quarters = new IntOpenHashSet();

	public CountQuartersOfDatesAggregator(SelectId id, Column column) {
		super(id, column);
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
	public CountQuartersOfDatesAggregator clone() {
		return new CountQuartersOfDatesAggregator(getId(), getColumn());
	}
}
