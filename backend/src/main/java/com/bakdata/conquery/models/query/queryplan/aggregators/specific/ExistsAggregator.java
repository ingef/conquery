package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Helper Aggregator, returning if it was used at least once.
 */
@RequiredArgsConstructor @ToString(of = {"requiredTables"})
public class ExistsAggregator implements Aggregator<Boolean> {

	private final Set<Table> requiredTables;


	public void setReference(QPNode ref) {
		this.reference = ref;
	}

	private QPNode reference;

	@Override
	public void acceptEvent(Bucket bucket, int event) {  }

	@Override
	public Boolean getAggregationResult() {
		return reference.isContained();
	}
	
	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public ExistsAggregator doClone(CloneContext ctx) {
		final ExistsAggregator existsAggregator = new ExistsAggregator(requiredTables);

		existsAggregator.setReference(ctx.clone(reference));

		return existsAggregator;
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.BooleanT.INSTANCE;
	}
}
