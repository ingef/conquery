package com.bakdata.conquery.models.query.queryplan.specific;

import java.time.LocalDate;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.RangeSet;

public class SpecialDateUnionAggregatorNode extends AggregatorNode<RangeSet<LocalDate>> {

	private Table requiredTable;
	
	public SpecialDateUnionAggregatorNode(Table requiredTable, SpecialDateUnion aggregator) {
		super(0, aggregator);
		this.requiredTable = requiredTable;
	}

	@Override
	public Multiset<Table> collectRequiredTables() {
		HashMultiset<Table> set = HashMultiset.create();
		set.add(requiredTable);
		return set;
	}
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		SpecialDateUnion aggClone = (SpecialDateUnion) clone.getAggregators().get(getPosition());
		return new SpecialDateUnionAggregatorNode(requiredTable, aggClone);
	}
}
