package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Helper Aggregator, returning a constant value passed in the constructor.
 */
@Getter
@AllArgsConstructor
@ToString(callSuper = true)
public class ConstantValueAggregator<T> extends Aggregator<T> {

	@Setter
	private T value;
	private final ResultType type;

	@Override
	public T createAggregationResult() {
		return value;
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {

	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {}
	
	@Override
	public ResultType getResultType() {
		return type;
	}
}
