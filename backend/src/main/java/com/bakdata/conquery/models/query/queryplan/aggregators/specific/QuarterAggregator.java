package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.Data;

/**
 * Samples the incoming dates outputting the year-quarter of the sample.
 */
@Data
public class QuarterAggregator implements Aggregator<String> {

	private final TemporalSampler sampler;

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	private Column column;

	public QuarterAggregator(TemporalSampler sampler) {
		this.sampler = sampler;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		column = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (getColumn() == null || !bucket.has(event, getColumn())) {
			return;
		}

		final CDateRange value = bucket.getAsDateRange(event, getColumn());

		if (value.isOpen()) {
			return;
		}

		set.maskedAdd(value, dateRestriction);
	}

	@Override
	public QuarterAggregator doClone(CloneContext ctx) {
		return new QuarterAggregator(sampler);
	}

	@Override
	public String getAggregationResult() {
		if (set.isEmpty()) {
			return null;
		}

		final OptionalInt sampled = sampler.sample(set);

		if (sampled.isEmpty()) {
			return null;
		}

		final LocalDate date = CDate.toLocalDate(sampled.getAsInt());
		final int quarter = QuarterUtils.getQuarter(date);
		final int year = date.getYear();

		return year + "-Q" + quarter;
	}

	@Override
	public ResultType getResultType() {
		return ResultType.StringT.INSTANCE;
	}
}
