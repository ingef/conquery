package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.function.Supplier;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Helper Aggregator, returning a constant value passed in the constructor.
 */
@Getter
@RequiredArgsConstructor
@ToString(callSuper = true)
public class SupplierValueAggregator<T> extends Aggregator<T> {

	private final Supplier<T> value;
	private final ResultType type;

	@Override
	public T createAggregationResult() {
		return value.get();
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
