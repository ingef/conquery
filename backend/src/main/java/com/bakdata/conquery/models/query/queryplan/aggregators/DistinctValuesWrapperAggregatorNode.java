package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

/**
 * Event is included only, when the value has not been seen before.
 */
public class DistinctValuesWrapperAggregatorNode<VALUE> extends ColumnAggregator<VALUE> {


	private final Aggregator<VALUE> aggregator;
	private Set<Object> observed = new HashSet<>();

	@Getter
	private final Column column;

	public DistinctValuesWrapperAggregatorNode(Aggregator<VALUE> aggregator, Column column) {
		this.column = column;
		this.aggregator = aggregator;
	}


	@Override
	public VALUE getAggregationResult() {
		return aggregator.getAggregationResult();
	}

	@Override
	public Column[] getRequiredColumns() {
		//TODO wrapped aggregator is ignored.
		return new Column[]{getColumn()};
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