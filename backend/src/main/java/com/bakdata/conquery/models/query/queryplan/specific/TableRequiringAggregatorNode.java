package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * An aggregator node that also requires a specific table from the query execution. This is necessary for 
 * the SpecialDateUnionAggregator. 
 */
public class TableRequiringAggregatorNode<T> extends AggregatorNode<T> {

	private TableId requiredTable;
	
	public TableRequiringAggregatorNode(TableId requiredTable, Aggregator<T> aggregator) {
		super(aggregator);
		this.requiredTable = requiredTable;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(requiredTable);
		getAggregator().collectRequiredTables(requiredTables);
	}
	
	@Override
	public TableRequiringAggregatorNode<T> doClone(CloneContext ctx) {
		return new TableRequiringAggregatorNode<T>(requiredTable, getAggregator().clone(ctx));
	}
}
