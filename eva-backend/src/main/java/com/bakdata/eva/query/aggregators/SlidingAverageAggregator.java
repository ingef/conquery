package com.bakdata.eva.query.aggregators;

import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.ColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public class SlidingAverageAggregator extends ColumnAggregator<Double> {

	private int count = 0;
	private double sum = 0;

	@NonNull
	private Column dateRangeColumn;

	@NonNull
	private Column valueColumn;

	@NonNull
	private Column maximumDaysColumn;

	private int quarter;

	@Override
	public Double getAggregationResult() {
		return count == 0 ? 0 : sum / (double) count;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		if(ctx.getDateRestriction().span().isAll())
			quarter = 1;
		else
			quarter = QuarterUtils.getQuarter(ctx.getDateRestriction().span().getMin());
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (
			!bucket.has(event, getDateRangeColumn())
			|| !bucket.has(event, getValueColumn())
			|| !bucket.has(event, getMaximumDaysColumn())) {
			return;
		}

		CDateRange dateRange = bucket.getDateRange(event, this.getDateRangeColumn());

		if (QuarterUtils.getQuarter(dateRange.getMin()) != quarter) {
			return;
		}


		double maxDays = bucket.getInteger(event, this.getMaximumDaysColumn());

		double value = bucket.getReal(event, this.getValueColumn());

		double durationInDays = dateRange.getNumberOfDays();
		double out = value * (Math.min(durationInDays, maxDays) / durationInDays);

		// Current Formula: value * (min(len(dateRange), maxDays) / len(dateRange))
		// Supposed Formula from the sources: value * (min(len(intersect(dateRange, valid)), maxDays) / len(dateRange))

		sum += out;
		count++;
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getDateRangeColumn(), getValueColumn(), getMaximumDaysColumn()};
	}

	@Override
	public SlidingAverageAggregator doClone(CloneContext ctx) {
		return new SlidingAverageAggregator(getDateRangeColumn(), getValueColumn(), getMaximumDaysColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
