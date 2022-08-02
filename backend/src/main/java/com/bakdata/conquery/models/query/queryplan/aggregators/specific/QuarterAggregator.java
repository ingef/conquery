package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;
import lombok.ToString;

/**
 * Samples the incoming dates outputting the year-quarter of the sample.
 */
@Data
@ToString(of = {"sampler", "column"})
public class QuarterAggregator extends Aggregator<String> {

	private final TemporalSamplerFactory samplerFactory;
	private TemporalSamplerFactory.Sampler sampler;

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	private Column column;

	public QuarterAggregator(TemporalSamplerFactory samplerFactory) {
		this.samplerFactory = samplerFactory;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
		sampler = samplerFactory.sampler();
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
	public String createAggregationResult() {
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
