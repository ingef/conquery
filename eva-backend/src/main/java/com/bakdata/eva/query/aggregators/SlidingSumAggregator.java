package com.bakdata.eva.query.aggregators;

import com.bakdata.conquery.models.common.CDate;
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
public class SlidingSumAggregator extends ColumnAggregator<Double> {

	private double sum = 0;

	@NonNull
	private Column dateRangeColumn;

	@NonNull
	private Column valueColumn;

	@NonNull
	private Column maximumDaysColumn;

	private int beginQuarter;
	private Column validityDateColumn;

	@Override
	public Double getAggregationResult() {
		return sum;
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		if(ctx.getDateRestriction().span().isAll())
			beginQuarter = 1;
		else
			beginQuarter = QuarterUtils.getQuarter(CDate.toLocalDate(ctx.getDateRestriction().getMinValue()));

		validityDateColumn = ctx.getValidityDateColumn();
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getDateRangeColumn())) {
			return;
		}

		if (!bucket.has(event, getValueColumn())) {
			return;
		}
		
		if (!bucket.has(event, getMaximumDaysColumn())) {
			return;
		}

		if(!bucket.has(event, getValidityDateColumn())) {
			return;
		}

		final CDateRange validity = bucket.getAsDateRange(event, getValidityDateColumn());


		final int currentQuarter = QuarterUtils.getQuarter(validity.getMin());

		if(beginQuarter != currentQuarter)
			return;

		CDateRange dateRange = bucket.getDateRange(event, getDateRangeColumn());

		double maxDays = bucket.getInteger(event, getMaximumDaysColumn());

		double value = bucket.getReal(event, getValueColumn());

		double durationInDays = dateRange.getNumberOfDays();
		double out = value * (Math.min(durationInDays, maxDays) / durationInDays);

		// Current Formula: value * (min(len(dateRange), maxDays) / len(dateRange))
		// Supposed Formula from the sources: value * (min(len(intersect(dateRange, valid)), maxDays) / len(dateRange))

		sum += out;
	}

	@Override
	public Column[] getRequiredColumns() {
		return new Column[]{getDateRangeColumn(), getValueColumn(), getMaximumDaysColumn()};
	}

	@Override
	public SlidingSumAggregator doClone(CloneContext ctx) {
		return new SlidingSumAggregator(getDateRangeColumn(), getValueColumn(), getMaximumDaysColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
