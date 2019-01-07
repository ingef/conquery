package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

import java.time.LocalDate;

public class SpecialDateUnion implements Aggregator<RangeSet<LocalDate>> {

	private RangeSet<LocalDate> set = TreeRangeSet.create();
	private Column currentColumn;

	@Override
	public void nextTable(QueryContext ctx, Table table) {
		currentColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (currentColumn != null) {
			CDateRange range = block.getAsDateRange(event, currentColumn);
			set.add(Range.closedOpen(range.getMin(), CDate.toLocalDate(range.getMaxValue() + 1)));
		}
	}

	@Override
	public SpecialDateUnion clone() {
		return new SpecialDateUnion();
	}

	public void merge(RangeSet<LocalDate> dates) {
		set.addAll(dates);
	}

	@Override
	public RangeSet<LocalDate> getAggregationResult() {
		return set;
	}
}
