package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import java.time.YearMonth;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjuster;

import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;

/**
 * Count the number of distinct quarters for all validity dates.
 */
public class CountQuartersAggregator implements Aggregator<Long> {

	private final TemporalAdjuster monthInQuarter = QuarterUtils.firstMonthInQuarterAdjuster();
	private final TemporalAdjuster nextQuarter = QuarterUtils.nextQuarterAdjuster();

	private final IntSet quarters = new IntOpenHashSet();

	@Getter
	private Column column;

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		this.column = ctx.getValidityDateColumn();

		if(!column.getType().isDateCompatible()){
			throw new IllegalStateException(String.format("Non date-compatible validityDate-Column[%s]", column));
		}
	}

	@Override
	public CountQuartersAggregator doClone(CloneContext ctx) {
		return new CountQuartersAggregator();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		CDateRange dateRange = bucket.getAsDateRange(event, column);

		if (dateRange.isOpen()) {
			return;
		}

		if (dateRange.isExactly()){
			quarters.add(dateRange.getMin().getYear() * 4 + dateRange.getMin().get(IsoFields.QUARTER_OF_YEAR));
			return;
		}


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
	public Long getAggregationResult() {
		return quarters.isEmpty() ? null : (long) quarters.size();
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
