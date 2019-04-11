package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.IsoFields;
import java.util.EnumSet;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.esotericsoftware.kryo.util.IntMap;

/**
 * Entity is included when the the number of quarters with events is within a
 * specified range.
 */
public class QuartersInYearAggregator extends SingleColumnAggregator<Long> {

	private final IntMap<EnumSet<Month>> quartersInYear = new IntMap<>();

	public QuartersInYearAggregator(Column column) {
		super(column);
	}

	@Override
	public void aggregateEvent(Block block, int event) {
		if (!block.has(event, getColumn())) {
			return;
		}

		LocalDate date = CDate.toLocalDate(block.getDate(event, getColumn()));

		EnumSet<Month> months = quartersInYear.get(date.getYear());
		int quarter = date.get(IsoFields.QUARTER_OF_YEAR);

		if (months == null) {
			months = EnumSet.of(QuarterUtils.getFirstMonthOfQuarter(quarter));
			quartersInYear.put(date.getYear(), months);
		}
		else {
			months.add(QuarterUtils.getFirstMonthOfQuarter(quarter));
		}
	}

	@Override
	public Long getAggregationResult() {
		long max = 0;

		for (EnumSet<Month> months : quartersInYear.values()) {
			long cardinality = months.size();
			if (cardinality > max) {
				max = cardinality;
			}

		}

		return max;
	}

	@Override
	public QuartersInYearAggregator doClone(CloneContext ctx) {
		return new QuartersInYearAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
