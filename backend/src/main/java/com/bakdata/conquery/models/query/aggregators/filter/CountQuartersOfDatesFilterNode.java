package com.bakdata.conquery.models.query.aggregators.filter;

import java.time.YearMonth;
import java.time.temporal.TemporalAdjuster;
import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.concepts.filters.specific.CountQuartersFilter;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.concept.filter.FilterValue;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

/**
 * Entity is included when the number of distinct quarters of the related events is within a given range.
 * Implementation is specific for LocalDates
 */
public class CountQuartersOfDatesFilterNode extends CountingAbstractFilterNode<CountQuartersFilter> {

	private static final TemporalAdjuster QUARTER_ADJUSTER = QuarterUtils.firstMonthInQuarterAdjuster();

	//TODO Consider making this a BitSet spanning a ~100 years - 4bit per year - and calculate positions manually
	private final Set<YearMonth> quarters = new HashSet<>();

	public CountQuartersOfDatesFilterNode(CountQuartersFilter countQuartersFilter, FilterValue.CQIntegerRangeFilter filterValue) {
		super(countQuartersFilter, filterValue);
	}

	@Override
	public CountQuartersOfDatesFilterNode clone(QueryPlan plan, QueryPlan clone) {
		return new CountQuartersOfDatesFilterNode(filter, filterValue);
	}

	@Override
	protected long update(Block block, int event) {
		if (block.has(event, filter.getColumn())) {
			quarters.add((YearMonth) QUARTER_ADJUSTER.adjustInto(CDate.toLocalDate(block.getDate(event, filter.getColumn()))));
		}

		return quarters.size();
	}
}
