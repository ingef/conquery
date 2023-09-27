package com.bakdata.conquery.apiv1.query.concept.specific.temporal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import lombok.Data;

@Data
public class TimeBasedQueryNode extends QPNode {

	private final Table table; // The AllIds Table

	private final TemporalSubQueryPlan subQuery;

	private boolean result;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		super.init(entity, context);

		// Yes, the subQuery is executed completely within the init stage of the outer Query.
		subQuery.init(context, entity);
		final Optional<?> execute = subQuery.execute(context, entity);

		result = execute.isPresent();
	}

	@Override
	public void collectRequiredTables(Set<Table> requiredTables) {
		requiredTables.add(table);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return subQuery.isOfInterest(entity);
	}


	@Override
	public void acceptEvent(Bucket bucket, int event) {
		// Does nothing
	}

	@Override
	public boolean isContained() {
		return result;
	}

	@Override
	public Collection<Aggregator<CDateSet>> getDateAggregators() {
		return List.of(subQuery.getIndexSubPlan().getDateAggregator());
	}
}
