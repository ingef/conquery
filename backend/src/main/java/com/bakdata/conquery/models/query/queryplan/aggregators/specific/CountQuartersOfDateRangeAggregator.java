package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjuster;

/**
 * Entity is included when the number of distinct quarters for all events is
 * within a given range. Implementation is specific for DateRanges
 */
public class CountQuartersOfDateRangeAggregator extends SingleColumnAggregator<Long> {

	private final TemporalAdjuster monthInQuarter = QuarterUtils.firstMonthInQuarterAdjuster();
	private final TemporalAdjuster nextQuarter = QuarterUtils.nextQuarterAdjuster();

	private final IntSet quarters = new IntOpenHashSet();

	public CountQuartersOfDateRangeAggregator(SelectId id, Column column) {
		super(id, column);
	}

	@Override
	public CountQuartersOfDateRangeAggregator clone() {
		return new CountQuartersOfDateRangeAggregator(getId(), getColumn());
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		CDateRange dateRange = block.getDateRange(event, getColumn());

		if (dateRange.isOpen()) {
			return;
		}

		YearMonth minQuarter = (YearMonth) monthInQuarter.adjustInto(dateRange.getMin());
		YearMonth maxQuarter = (YearMonth) monthInQuarter.adjustInto(dateRange.getMax());

		if (minQuarter.equals(maxQuarter)) {
			quarters.add(minQuarter.getYear() * 4 + minQuarter.get(IsoFields.QUARTER_OF_YEAR));
		}
		else {
			YearMonth quarter = minQuarter;
			// Iterate with max inclusive
			while (!quarter.isAfter(maxQuarter)) {
				quarters.add(quarter.getYear() * 4 + quarter.get(IsoFields.QUARTER_OF_YEAR));

				quarter = (YearMonth) nextQuarter.adjustInto(quarter);
			}
		}
	}

	@Override
	public Long getAggregationResult() {
		return (long) quarters.size();
	}
}
