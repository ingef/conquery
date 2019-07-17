package com.bakdata.eva.query.aggregators;

import java.time.LocalDate;
import java.time.temporal.IsoFields;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.Getter;

@Getter
public class PeriodAverageAggregator extends SingleColumnAggregator<Double> {

	private double sum = 0;
	private long includedQuarters = 0;

	private Column validityDate;
	private CDateSet dateRestriction;

	private LocalDate beginRestriction;
	private LocalDate endRestriction;

	private long nQuarters;
	private double overHangWeight;

	private boolean odd;
	private boolean hit = false;

	public PeriodAverageAggregator(@NsIdRef Column column) {
		super(column);
	}

	@Override
	public Double getAggregationResult() {
		if(!hit || nQuarters < 4) {
			return null;
		}

		return sum / (double) includedQuarters;
	}

	private long quartersBetween(LocalDate begin, LocalDate end) {
		return IsoFields.QUARTER_YEARS.between(begin, end);
	}

	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		validityDate = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();

		beginRestriction = QuarterUtils.getFirstDayOfQuarter(dateRestriction.getMinValue());
		endRestriction = QuarterUtils.getLastDayOfQuarter(dateRestriction.getMaxValue()).plusDays(1);

		nQuarters = quartersBetween(beginRestriction, endRestriction);
		odd = nQuarters % 4 != 0;
		overHangWeight = nQuarters - Math.floor(nQuarters / 4d) * 4;
	}

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		if (!bucket.has(event, getValidityDate())) {
			return;
		}

		hit = true;

		final LocalDate date = bucket.getAsDateRange(event, getValidityDate()).getMin();
		final double value = bucket.getReal(event, getColumn());

		final long quartersFromBegin = quartersBetween(beginRestriction, date);
		final long quartersToEnd = quartersBetween(date, endRestriction);

		if (quartersFromBegin % 4 == 0) {
			if (quartersToEnd >= 4) {
				this.includedQuarters += 4;
				sum += value * 4;
			}
		}
		else if (odd && quartersToEnd == 4) {
			this.includedQuarters += overHangWeight;
			sum += value * overHangWeight;
		}
	}

	@Override
	public PeriodAverageAggregator doClone(CloneContext context) {
		return new PeriodAverageAggregator(getColumn());
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.NUMERIC;
	}
}
