package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import java.util.Collection;
import java.util.Collections;

public class Leaf extends QPNode {

	private boolean triggered = false;
	
	@Override
	public QPNode doClone(CloneContext ctx) {
		return new Leaf();
	}
	
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		triggered = true;
	}

	@Override
	public boolean isContained() {
		return triggered;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return Collections.emptySet();
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return true;
	}
	
	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}
}
