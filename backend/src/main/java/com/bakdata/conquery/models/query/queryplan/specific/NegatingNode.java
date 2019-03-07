package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;

import lombok.NonNull;

public class NegatingNode extends QPChainNode {

	public NegatingNode(@NonNull QPNode child) {
		super(child);
	}
	
	@Override
	public boolean nextEvent(Block block, int event) {
		getChild().aggregate(block, event);
		return true;
	}
	
	@Override
	public QPNode clone(QueryPlan plan, ConceptQueryPlan clone) {
		return new NegatingNode(getChild().clone(plan, clone));
	}
	
	@Override
	public boolean isContained() {
		return !getChild().isContained();
	}
}
