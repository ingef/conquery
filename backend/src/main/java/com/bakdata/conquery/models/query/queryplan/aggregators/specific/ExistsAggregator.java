package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.RequiredArgsConstructor;

/**
 * Helper Aggregator, returning if it was used at least once.
 */
@RequiredArgsConstructor
public class ExistsAggregator implements Aggregator<Boolean> {

	private final Set<TableId> requiredTables;
	private boolean hit = false;

	@Override
	public void aggregateEvent(Bucket bucket, int event) {
		hit = true;
	}

	@Override
	public Boolean getAggregationResult() {
		return hit;
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public ExistsAggregator doClone(CloneContext ctx) {
		return new ExistsAggregator(requiredTables);
	}
	
	@Override
	public ResultType getResultType() {
		return ResultType.BOOLEAN;
	}
	
	@Override
	public String toString(){
		return getClass().getSimpleName();
	}
	@Override
	public void reset() {
		requiredTables.clear();
		hit = false;
	
	}
}
