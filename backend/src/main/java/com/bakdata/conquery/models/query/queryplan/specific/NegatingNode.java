package com.bakdata.conquery.models.query.queryplan.specific;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.DateAggregator;
import com.bakdata.conquery.models.query.queryplan.QPChainNode;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.NonNull;

public class NegatingNode extends QPChainNode {

	private final DateAggregator dateAggregator;

	public NegatingNode(@NonNull QPNode child, @NonNull DateAggregationAction dateAction) {
		super(child);
		this.dateAggregator = new DateAggregator(dateAction);
		dateAggregator.registerAll(child.getDateAggregators());
	}

	private NegatingNode(@NonNull QPNode child, @NonNull DateAggregator dateAggregator) {
		super(child);
		this.dateAggregator = dateAggregator;
	}
	
	@Override
	public void acceptEvent(Bucket bucket, int event) {
		getChild().acceptEvent(bucket, event);
	}

	@Override
	public boolean isContained() {
		return !getChild().isContained();
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		if (dateAggregator != null && dateAggregator.hasChildren()) {
			return Set.of(dateAggregator);
		}
		return Collections.emptySet();
	}
}
