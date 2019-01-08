package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @Getter @ToString(of = "aggregator")
public class AggregatorNode<T> extends QPNode  {

	private final int position;
	private final Aggregator<T> aggregator;
	private boolean triggered = false;
	
	@Override
	public boolean nextEvent(Block block, int event) {
		triggered = true;
		aggregator.aggregateEvent(block, event);
		return true;
	}

	@Override
	public boolean isContained() {
		return triggered;
	}
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		Aggregator<T> aggClone = (Aggregator<T>) clone.getAggregators().get(position);
		return new AggregatorNode<>(position, aggClone);
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		aggregator.collectRequiredTables(requiredTables);
	}
	
	@Override
	public void nextBlock(Block block) {
		aggregator.nextBlock(block);
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		aggregator.nextTable(ctx, currentTable);
	}
}
