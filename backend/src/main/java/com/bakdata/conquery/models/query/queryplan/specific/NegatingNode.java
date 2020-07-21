package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.NonNull;

public class NegatingNode extends QPChainNode {

	public NegatingNode(@NonNull QPNode child) {
		super(child);
	}
	
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		getChild().acceptEvent(bucket, event);
	}
	
	@Override
	public NegatingNode doClone(CloneContext ctx) {
		return new NegatingNode(ctx.clone((QPNode) getChild()));
	}
	
	@Override
	public boolean isContained() {
		return !getChild().isContained();
	}
}
