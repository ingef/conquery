package com.bakdata.conquery.models.query.queryplan.aggregators.specific;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.types.ResultType;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Helper Aggregator, returning if it was used at least once.
 */
@RequiredArgsConstructor
@ToString(of = "requiredTables")
public class ExistsAggregator extends Aggregator<Boolean> {

	private final Set<Table> requiredTables;


	public void setReference(QPNode ref) {
		reference = ref;
	}

	private QPNode reference;

	@Override
	public void consumeEvent(Bucket bucket, int event) {  }

	@Override
	public Boolean createAggregationResult() {
		return reference.isContained();
	}
	
	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}

	@Override
	public void init(Entity entity, QueryExecutionContext context) {

	}

	@Override
	public ResultType getResultType() {
		return ResultType.BooleanT.INSTANCE;
	}
}
