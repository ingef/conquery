package com.bakdata.conquery.models.query.aggregators.filter;

import java.time.YearMonth;
import java.time.temporal.TemporalAdjuster;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;


/**
 * Entity is included when the number of distinct quarters for all events is within a given range.
 * Implementation is specific for DateRanges
 */
public class CountQuartersOfDateRangeFilterNode extends CountingAbstractFilterNode<CountQuartersFilter> {

	private final TemporalAdjuster monthInQuarter = QuarterUtils.firstMonthInQuarterAdjuster();
	private final TemporalAdjuster nextQuarter = QuarterUtils.nextQuarterAdjuster();
	//TODO Consider making this a BitSet spanning a ~100 years - 4bit per year - and calculate positions manually
	private final Set<YearMonth> quarters = new HashSet<>();

	public CountQuartersOfDateRangeFilterNode(CountQuartersFilter countQuartersFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(countQuartersFilter, filterValue);
	}

	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new CountQuartersOfDateRangeFilterNode(filter, filterValue);
	}

	@Override
	protected long update(Block block, int event) {
		if (block.has(event, filter.getColumn())) {
			CDateRange dateRange = block.getDateRange(event, filter.getColumn());

			YearMonth minQuarter = (YearMonth) monthInQuarter.adjustInto(dateRange.getMin());
			YearMonth maxQuarter = (YearMonth) monthInQuarter.adjustInto(dateRange.getMax());

			if (minQuarter.equals(maxQuarter)) {
				quarters.add(minQuarter);
			}
			else {
				YearMonth quarter = minQuarter;
				// Iterate with max inclusive
				while (!quarter.isAfter(maxQuarter)) {
					quarters.add(quarter);

					quarter = (YearMonth) nextQuarter.adjustInto(quarter);
				}
			}
		}

		return quarters.size();
	}
}
