package com.bakdata.conquery.models.query.queryplan;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter(AccessLevel.PROTECTED) @Setter(AccessLevel.PROTECTED)
public abstract class QPNode extends EventIterating {
	protected QueryExecutionContext context;
	protected Entity entity;

	/**
	 * Initialize the QueryPlan element for evaluation. eg.: Prefetching elements.
	 * @apiNote inheritors should always call super.
	 */
	public void init(Entity entity, QueryExecutionContext context) {
		setEntity(entity);
		setContext(context);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		setContext(ctx);
	}

	@Override
	public abstract void acceptEvent(Bucket bucket, int event);

	public abstract boolean isContained();

	public List<QPNode> getChildren() {
		return Collections.emptyList();
	}

	/**
	 * Retrieves all generated date Aggregator from the lower level of the tree.
	 * This is builds a parallel tree to the actual query tree to generate the dates column in the final result.
	 * The aggregator are registered in the date aggregator of the upper level (see @{@link DateAggregator#registerAll(Collection)})
	 */
	public abstract Collection<Aggregator<CDateSet>> getDateAggregators();
}
