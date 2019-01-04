package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.google.common.collect.Multiset;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class AggregatorNode<T> extends QPNode  {

	private final int position;
	private final Aggregator<T> aggregator;
	private boolean triggered = false;
	
	@Override
	protected OpenResult nextEvent(Block block, int event) {
		triggered = true;
		aggregator.aggregateNextEvent(block, event);
		return OpenResult.MAYBE;
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
	public Multiset<Table> collectRequiredTables() {
		return aggregator.collectRequiredTables();
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
