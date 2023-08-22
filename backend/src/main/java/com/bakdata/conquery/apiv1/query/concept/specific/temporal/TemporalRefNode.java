package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.Collection;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.Data;

@Data
public class TemporalRefNode extends QPNode {

	private final CQAbstractTemporalQuery ref;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {

	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		// Does nothing
	}

	@Override
	public boolean isContained() {
		return context.getTemporalQueryResult().getOrDefault(ref, false);
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return null;
	}
}
