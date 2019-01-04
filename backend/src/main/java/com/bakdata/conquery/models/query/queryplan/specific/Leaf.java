package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;

public class Leaf extends QPNode {

	private boolean triggered = false;
	
	@Override
	public QPNode clone(QueryPlan plan, QueryPlan clone) {
		return new Leaf();
	}
	
	@Override
	public boolean nextEvent(Block block, int event) {
		triggered = true;
		return true;
	}

	@Override
	public boolean isContained() {
		return triggered;
	}

	
}
