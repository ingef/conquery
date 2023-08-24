package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SupplierValueAggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.Data;

@Data
public class TemporalRefNode extends QPNode {

	private final Table table; // The AllIds Table
	private final CQTemporal ref;

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(table);
	}

	@Override
	public boolean isOfInterest(Entity bucket) {
		return context.getTemporalQueryResult().containsKey(ref);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		// Does nothing
	}

	@Override
	public boolean isContained() {
		return context.getTemporalQueryResult().containsKey(ref);
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return List.of(new SupplierValueAggregator<>(() -> context.getTemporalQueryResult().get(ref), new ResultType.ListT(ResultType.DateRangeT.INSTANCE)));
	}
}
