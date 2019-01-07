package com.bakdata.conquery.models.query.queryplan.specific;

import java.time.LocalDate;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.google.common.collect.RangeSet;

public class SpecialDateUnionAggregatorNode extends AggregatorNode<RangeSet<LocalDate>> {

	private Table requiredTable;
	
	public SpecialDateUnionAggregatorNode(Table requiredTable, SpecialDateUnion aggregator) {
		super(0, aggregator);
		this.requiredTable = requiredTable;
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(requiredTable);
	}
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		SpecialDateUnion aggClone = (SpecialDateUnion) clone.getAggregators().get(getPosition());
		return new SpecialDateUnionAggregatorNode(requiredTable, aggClone);
	}
}
