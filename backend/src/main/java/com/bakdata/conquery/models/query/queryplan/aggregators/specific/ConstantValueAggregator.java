package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Helper Aggregator, returning a constant value passed in the constructor.
 */
@Getter
@ToString(callSuper = true)
@AllArgsConstructor
public class ConstantValueAggregator<T> extends Aggregator<T> {

	@Setter
	private T value;


	@Override
	public T createAggregationResult() {
		return value;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {

	}

	@Override
	public void consumeEvent(Bucket bucket, int event) {
	}

}
