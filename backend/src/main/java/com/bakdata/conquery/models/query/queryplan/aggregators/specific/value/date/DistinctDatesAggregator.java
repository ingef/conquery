package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class DistinctDatesAggregator extends SingleColumnAggregator<List<LocalDate>> {

	private final List<LocalDate> entries = new ArrayList<>();

	public DistinctDatesAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (block.has(event, getColumn())) {
			entries.add(CDate.toLocalDate(block.getDate(event, getColumn())));
		}
	}

	@Override
	public List<LocalDate> getAggregationResult() {
		return entries;
	}

	@Override
	public DistinctDatesAggregator clone() {
		return new DistinctDatesAggregator(getColumn());
	}
}
