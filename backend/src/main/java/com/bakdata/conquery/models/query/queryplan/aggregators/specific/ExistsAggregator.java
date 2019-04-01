package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExistsAggregator implements Aggregator<Boolean> {

	private final Set<TableId> requiredTables;
	private boolean hit = false;

	@Override
	public void aggregateEvent(Block block, int event) {
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
}
