package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.concept.specific.temporal.TemporalSampler;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Aggregator, counting the number of days present.
 */
public class QuarterAggregator extends SingleColumnAggregator<Integer> {

	private final TemporalSampler sampler;

	private CDateSet set = CDateSet.create();
	private CDateSet dateRestriction;

	public QuarterAggregator(Column column, TemporalSampler sampler) {
		super(column);
		this.sampler = sampler;
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		dateRestriction = ctx.getDateRestriction();
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
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
		return new QuarterAggregator(getColumn(), sampler);
	}

	@Override
	public Integer getAggregationResult() {
		if (set.isEmpty()) {
			return null;
		}
		final OptionalInt sampled = sampler.sample(set);

		if (sampled.isEmpty()) {
			return null;
		}

		return sampled.getAsInt();
	}

	@Override
	public ResultType getResultType() {
		return ResultType.DateT.INSTANCE;
	}
}
