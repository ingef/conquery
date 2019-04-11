package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.NonNull;

public class NegatingNode extends QPChainNode {

	public NegatingNode(@NonNull QPNode child) {
		super(child);
	}
	
	@Override
	public void nextEvent(Block block, int event) {
		getChild().nextEvent(block, event);
	}
	
	@Override
	public NegatingNode doClone(CloneContext ctx) {
		return new NegatingNode(getChild().clone(ctx));
	}
	
	@Override
	public boolean isContained() {
		return !getChild().isContained();
	}
}
