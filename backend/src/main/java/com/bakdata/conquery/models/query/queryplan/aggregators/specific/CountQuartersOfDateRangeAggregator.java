package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjuster;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.ToString;

/**
 * Count the number of distinct quarters for all events. Implementation is specific for DateRanges
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class CountQuartersOfDateRangeAggregator extends SingleColumnAggregator<Long> {

	private final TemporalAdjuster monthInQuarter = QuarterUtils.firstMonthInQuarterAdjuster();
	private final TemporalAdjuster nextQuarter = QuarterUtils.nextQuarterAdjuster();

	private final IntSet quarters = new IntOpenHashSet();
	private CDateSet dateRestriction;

	public CountQuartersOfDateRangeAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		quarters.clear();
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		final CDateSet set = CDateSet.create(bucket.getDateRange(event, getColumn()));
		set.retainAll(dateRestriction);

		for (CDateRange subRange : set.asRanges()) {
			// we can sensibly only look at real quarters.
			if(subRange.isOpen()){
				continue;
			}

			addDateRange(subRange);
		}
	}

	public void addDateRange(CDateRange dateRange) {
		YearMonth minQuarter = YearMonth.from(monthInQuarter.adjustInto(dateRange.getMin()));
		YearMonth maxQuarter = YearMonth.from(monthInQuarter.adjustInto(dateRange.getMax()));

		if (minQuarter.equals(maxQuarter)) {
			quarters.add(minQuarter.get(ChronoField.YEAR) * 4 + minQuarter.get(IsoFields.QUARTER_OF_YEAR));
		}
		else {
			YearMonth quarter = minQuarter;
			// Iterate with max inclusive
			while (!quarter.isAfter(maxQuarter)) {
				quarters.add(quarter.getYear() * 4 + quarter.get(IsoFields.QUARTER_OF_YEAR));

				quarter = YearMonth.from(nextQuarter.adjustInto(quarter));
			}
		}
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
