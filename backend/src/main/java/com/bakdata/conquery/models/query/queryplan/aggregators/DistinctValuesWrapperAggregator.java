package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Helper Aggregator, forwarding only events with distinct values to {@code aggregator}.
 * @param <VALUE>
 */
public class DistinctValuesWrapperAggregator<VALUE> extends ColumnAggregator<VALUE> {

	private final ColumnAggregator<VALUE> aggregator;
	private final Set<Object> observed = new HashSet<>();

	@Getter
	private final Column column;

	public DistinctValuesWrapperAggregator(ColumnAggregator<VALUE> aggregator, Column column) {
		this.column = column;
		this.aggregator = aggregator;
	}

	@Override
	public VALUE getAggregationResult() {
		return aggregator.getAggregationResult();
	}

	@Override
	public Column[] getRequiredColumns() {
		return ArrayUtils.add(aggregator.getRequiredColumns(), getColumn());
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		if(!bucket.has(event,getColumn())){
			return;
		}

		if (observed.add(bucket.createScriptValue(event, getColumn()))) {
			aggregator.acceptEvent(bucket, event);
		}
	}

	@Override
	public ResultType getResultType() {
		return aggregator.getResultType();
	}
}