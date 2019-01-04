package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public class Leaf extends QPNode {

	private boolean triggered = false;
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new Leaf();
	}

	@Override
	public Multiset<Table> collectRequiredTables() {
		return HashMultiset.create();
	}

	@Override
	protected OpenResult nextEvent(Block block, int event) {
		triggered = true;
		return OpenResult.INCLUDED;
	}

	@Override
	public boolean isContained() {
		return triggered;
	}
}
