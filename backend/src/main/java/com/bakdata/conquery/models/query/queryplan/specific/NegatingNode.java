package com.bakdata.conquery.models.query.queryplan.specific;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.*;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class NegatingNode extends QPChainNode {

	private final DateAggregator dateAggregator;

	public NegatingNode(@NonNull QPNode child, DateAggregationAction dateAction) {
		super(child);
		if (dateAction != null) {
			this.dateAggregator = new DateAggregator(dateAction);
			dateAggregator.register(child.getDateAggregators());
		}
		else {
			this.dateAggregator = null;
		}
	}

	private NegatingNode(@NonNull QPNode child, DateAggregator dateAggregator) {
		super(child);
		this.dateAggregator = dateAggregator;
	}
	
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		getChild().acceptEvent(bucket, event);
	}
	
	@Override
	public NegatingNode doClone(CloneContext ctx) {
		return new NegatingNode(ctx.clone(getChild()), this.dateAggregator != null ? ctx.clone(this.dateAggregator): null);
	}
	
	@Override
	public boolean isContained() {
		return !getChild().isContained();
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		if (dateAggregator != null) {
			return Set.of(dateAggregator);
		}
		return Collections.emptySet();
	}
}
