package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;

import java.util.Set;

public class SpecialDateUnionAggregatorNode extends AggregatorNode<CDateSet> {

	private TableId requiredTable;
	
	public SpecialDateUnionAggregatorNode(TableId requiredTable, SpecialDateUnion aggregator) {
		super(0, aggregator);
		this.requiredTable = requiredTable;
	}

	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.add(requiredTable);
	}
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		SpecialDateUnion aggClone = (SpecialDateUnion) clone.getAggregators().get(getAggregator().getId());
		return new SpecialDateUnionAggregatorNode(requiredTable, aggClone);
	}
}
