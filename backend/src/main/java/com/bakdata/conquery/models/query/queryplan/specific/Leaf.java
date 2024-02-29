package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
import java.util.Collections;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.ToString;

@ToString
public class Leaf extends QPNode {

	private boolean triggered = false;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);
		triggered = false;
	}

	@Override
	public boolean acceptEvent(Bucket bucket, int event) {
		triggered = true;
		return true;
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
