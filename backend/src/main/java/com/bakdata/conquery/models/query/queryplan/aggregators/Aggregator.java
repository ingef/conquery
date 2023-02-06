package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.types.ResultType;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An aggregator iterates over events, computing a value alongside. Values are fed through {@link Aggregator#acceptEvent(Bucket, int)}, and the result can be queried at {@link Aggregator#createAggregationResult()}.
 * <p>
 * Every Aggregator has an associated {@code ResultType} that is used for rendering purposes.
 * <p>
 * See Also {@code EventIterating}.
 * <p>
 * Aggregators are used to produce additional values for query results, but are also used in filters, to restrict the result set.
 * <p>
 * An Aggregator is usually created by an associated {@code Select}, which is their API Layer counterpart.
 *
 * @param <T> Java result type after aggregation.
 */
public abstract class Aggregator<T> extends EventIterating {

	/**
	 * Compute aggregation result.
	 *
	 * @implSpec Returned objects may not be reused in later Query evaluation, therefore createAggregationResult must copy collections when they are returned.
	 */
	public abstract T createAggregationResult();

	/**
	 * Specific type of the result used for rendering.
	 */
	@JsonIgnore
	public abstract ResultType getResultType();

}
