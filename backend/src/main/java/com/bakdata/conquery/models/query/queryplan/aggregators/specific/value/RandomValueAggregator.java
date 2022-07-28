package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import java.util.Random;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.ToString;

/**
 * Aggregator, returning a random value of a column.
 *
 * @param <VALUE> Value type of the column/return value
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class RandomValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private final Random random = new Random(ConqueryConstants.RANDOM_SEED);
	private int event;
	private int nValues = 0;
	private Bucket bucket;

	public RandomValueAggregator(Column column) {
		super(column);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		event = -1;
		nValues = 0;
		bucket = null;
	}

	/**
	 * length of sequence = m, but not known at event of sampling
	 * <p>
	 * P(switching n-th value) = 1/n
	 * <p>
	 * P(n-th value = output) 	= P(switching n-th value) * P(not switching values > n)
	 * = 1/n * n/m = 1/m
	 *
	 * @param bucket
	 * @param event
	 */
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return;
		}

		// Count how many values we have seen, so a draw is always evenly distributed
		nValues++;

		if (random.nextInt(nValues) == 0) {
			this.event = event;
			this.bucket = bucket;
		}
	}

	@Override
	public VALUE createAggregationResult() {
		if (bucket == null) {
			return null;
		}

		return (VALUE) bucket.createScriptValue(event, getColumn());
	}

	@Override
	public ResultType getResultType() {
		return ResultType.resolveResultType(getColumn().getType());
	}
}
