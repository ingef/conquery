package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

public class SpecialDateUnionAggregatorNode extends AggregatorNode<String> {

	private TableId requiredTable;
	
	public SpecialDateUnionAggregatorNode(TableId requiredTable, SpecialDateUnion aggregator) {
		super(aggregator);
		this.requiredTable = requiredTable;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(requiredTable);
	}
	
	@Override
	public SpecialDateUnionAggregatorNode doClone(CloneContext ctx) {
		SpecialDateUnion aggClone = (SpecialDateUnion) getAggregator().clone(ctx);
		return new SpecialDateUnionAggregatorNode(requiredTable, aggClone);
	}
}
