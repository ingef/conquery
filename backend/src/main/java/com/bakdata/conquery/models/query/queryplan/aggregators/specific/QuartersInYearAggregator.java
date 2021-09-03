package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.IsoFields;
import java.util.EnumSet;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Aggregator counting the number of quarters in a year, returning the maximum number of quarters present.
 */
public class QuartersInYearAggregator extends SingleColumnAggregator<Long> {

	private final Int2ObjectMap<EnumSet<Month>> quartersInYear = new Int2ObjectOpenHashMap<>();

	public QuartersInYearAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		quartersInYear.clear();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		LocalDate date = CDate.toLocalDate(bucket.getDate(event, getColumn()));

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
	public Long createAggregationResult() {
		if(quartersInYear.isEmpty()) {
			return null;
		}

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
	public ResultType getResultType() {
		return ResultType.IntegerT.INSTANCE;
	}
}
