package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

public class DurationSumAggregatorNode extends SingleColumnAggregator<Long> {

	private RangeSet<Integer> set = TreeRangeSet.create();
	private CDateRange dateRestriction;

	public DurationSumAggregatorNode(Column column) {
		super(column);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		CDateRange range = block.getAsDateRange(event, getColumn());

		if (dateRestriction != null) {
			if (range.intersects(dateRestriction)) {
				range = dateRestriction.intersection(range);
			}
			else {
				return;
			}
		}

		// This could backfire heavily.
		if (range.isOpen()) {
			return;
		}

		set.add(Range.closedOpen(range.getMinValue(), range.getMaxValue() + 1));
	}

	@Override
	public DurationSumAggregatorNode clone() {
		return new DurationSumAggregatorNode(getColumn());
	}

	@Override
	public Long getAggregationResult() {
		long sum = 0;

		for (Range<Integer> range : set.asRanges()) {
			sum += range.upperEndpoint() - range.lowerEndpoint();
		}

		return sum;
	}
}
