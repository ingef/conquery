package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.OpenResult;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

import lombok.NonNull;

public class NegatingNode extends QPChainNode {

	public NegatingNode(@NonNull QPNode child) {
		super(child);
	}
	
	@Override
	protected OpenResult nextEvent(Block block, int event) {
		switch (getChild().aggregate(block, event)) {
			case NOT_INCLUDED:
				return OpenResult.INCLUDED;
			case INCLUDED:
				return OpenResult.NOT_INCLUDED;
			case MAYBE:
				return OpenResult.MAYBE;
			default:
				throw new IllegalStateException("unknown state for NegatingAggregator "+this.toString());
		}
	}
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new NegatingNode(getChild().clone(plan, clone));
	}
	
	@Override
	public boolean isContained() {
		return !getChild().isContained();
	}
}
