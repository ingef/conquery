package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.externalservice.SimpleResultType;
import com.bakdata.conquery.models.query.queryplan.EventIterating;
import com.bakdata.conquery.models.query.queryplan.clone.CtxCloneable;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * An aggregator iterates over events, computing a value alongside. Values are fed through {@code aggregateEvent}, and the result can be queried at {@code getAggregationResult}.
 *
 * Every Aggregator has an associtaed {@code ResultType} that is used for rendering purposes.
 *
 * See Also {@code EventIterating} and {@code CtxCloneable}.
 *
 * Aggregators are used to produce additional values for query results, but are also used in filters, to restrict the result set.
 *
 * An Aggregator is usually created by an associated {@code Select}, which is their API Layer counterpart.
 *
 * @param <T> Java result type after aggregation.
 */
public interface Aggregator<T> extends CtxCloneable<Aggregator<T>>, EventIterating {

	T getAggregationResult();

	@JsonIgnore
    SimpleResultType getResultType();

}
