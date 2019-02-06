package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value.date;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity is included when the number of values for a specified column are
 * within a given range.
 */
public class AllDatesAggregator extends SingleColumnAggregator<List<LocalDate>> {

	private final List<LocalDate> entries = new ArrayList<>();

	public AllDatesAggregator(SelectId id, Column column) {
		super(id, column);
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
	public AllDatesAggregator clone() {
		return new AllDatesAggregator(getId(), getColumn());
	}
}
