package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.time.LocalDate;
import java.util.OptionalInt;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.TemporalSamplerFactory;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
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

	private CDateSet set = CDateSet.createEmpty();
	private CDateSet dateRestriction;

	private ValidityDate validityDate;

	private int realUpperBound;

	public QuarterAggregator(TemporalSamplerFactory samplerFactory) {
		this.samplerFactory = samplerFactory;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		set.clear();
		realUpperBound = context.getToday();
		sampler = samplerFactory.sampler(realUpperBound);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		validityDate = ctx.getValidityDateColumn();
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		final CDateRange dateRange = validityDate.getValidityDate(event, bucket);

		if (dateRange == null){
			return;
		}

		set.maskedAdd(dateRange, dateRestriction, realUpperBound);
	}

	@Override
	public String createAggregationResult() {
		if (set.isEmpty()) {
			return null;
		}

		final OptionalInt maybeSampled = sampler.sample(set);

		if (maybeSampled.isEmpty()) {
			return null;
		}

		final int sampled = maybeSampled.getAsInt();

		if (CDate.isNegativeInfinity(sampled) || CDate.isPositiveInfinity(sampled)) {
			return null;
		}

		final LocalDate date = CDate.toLocalDate(sampled);
		final int quarter = QuarterUtils.getQuarter(date);
		final int year = date.getYear();

		return year + "-Q" + quarter;
	}

	@Override
	public ResultType getResultType() {
		return ResultType.StringT.INSTANCE;
	}
}
