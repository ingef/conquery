package com.bakdata.conquery.models.query.queryplan.aggregators;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.google.common.collect.Multiset;

public interface Aggregator<T> extends Cloneable {

	public default void nextTable(QueryContext ctx, Table table) {}
	
	public default void nextBlock(Block block) {}
	
	void aggregateNextEvent(Block block, int event);
	
	T getAggregationResult();
	
	public Aggregator<T> clone();
	
	Multiset<Table> collectRequiredTables();
}
