package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;

import lombok.Getter;

/**
 * Aggregator node forwarding only events with distinct values to {@code aggregator}.
 * @param <VALUE>
 */
public class DistinctValuesWrapperAggregatorNode<VALUE> extends ColumnAggregator<VALUE> {

	private final ColumnAggregator<VALUE> aggregator;
	private Set<Object> observed = new HashSet<>();

	@Getter
	private final Column column;

	public DistinctValuesWrapperAggregatorNode(ColumnAggregator<VALUE> aggregator, Column column) {
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
	public void aggregateEvent(Block block, int event) {
		if (observed.add(block.getAsObject(event, getColumn()))) {
			aggregator.aggregateEvent(block, event);
		}
	}

	@Override
	public ColumnAggregator<VALUE> clone() {
		return new DistinctValuesWrapperAggregatorNode<>(aggregator.clone(), column);
	}
}