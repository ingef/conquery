package com.bakdata.conquery.models.query.queryplan.aggregators.specific.date;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.IsoFields;
import java.util.EnumSet;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * Aggregator counting the number of quarters in a year, returning the maximum number of quarters present.
 */
public class QuartersInYearAggregator implements Aggregator<Long> {

	private final Int2ObjectMap<EnumSet<Month>> quartersInYear = new Int2ObjectOpenHashMap<>();

	private Column column;

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		column = ctx.getValidityDateColumn();

		if(!column.getType().isDateCompatible()){
			throw new IllegalStateException(String.format("Non date-compatible validityDate-Column[%s]", column));
		}
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, column)) {
			return;
		}

		LocalDate date = CDate.toLocalDate(bucket.getDate(event, column));

		EnumSet<Month> months = quartersInYear.computeIfAbsent(date.getYear(), y -> EnumSet.noneOf(Month.class));
		months.add(QuarterUtils.getFirstMonthOfQuarter(date.get(IsoFields.QUARTER_OF_YEAR)));
	}

	@Override
	public Long getAggregationResult() {
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
	public QuartersInYearAggregator doClone(CloneContext ctx) {
		return new QuartersInYearAggregator();
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.INTEGER;
	}
}
