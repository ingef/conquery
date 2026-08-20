package com.bakdata.conquery.models.query.queryplan.aggregators.specific.value;

import static com.bakdata.conquery.models.query.StringUtils.getSubstringFromRange;

import java.util.Random;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.SingleColumnAggregator;
import lombok.ToString;

/**
 * Aggregator, returning a random value of a column.
 *
 * @param <VALUE> Value type of the column/return value
 */
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class RandomValueAggregator<VALUE> extends SingleColumnAggregator<VALUE> {

	private final Range.IntegerRange substring;

	private Random random;
	private int event;
	private int nValues = 0;
	private Bucket bucket;

	public RandomValueAggregator(Column column, Range.IntegerRange substring) {
		super(column);
		this.substring = substring;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		event = -1;
		nValues = 0;
		bucket = null;
		random = new Random(ConqueryConstants.RANDOM_SEED);
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
	public void consumeEvent(Bucket bucket, int event) {
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

		if (substring != null) {
			String string = bucket.getString(event, getColumn());
			return (VALUE) getSubstringFromRange(string, substring);
		}

		return (VALUE) bucket.createScriptValue(event, getColumn());
	}

}
