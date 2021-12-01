package com.bakdata.conquery.models.query.filter;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.filter.FilterNode;
import lombok.Getter;

/**
 * Abstract class for filter nodes acting on aggregation results.
 *
 * @param <AGGREGATOR>   Type of the Aggregator
 * @param <FILTER_VALUE> Type of the used FilterValue
 */
public abstract class AggregationResultFilterNode<AGGREGATOR extends Aggregator<?>, FILTER_VALUE> extends FilterNode<FILTER_VALUE> {

	@Getter
	private AGGREGATOR aggregator;

	@Override
	public void init(Entity entity, QueryExecutionContext context) {
		aggregator.init(entity, context);
	}

	public AggregationResultFilterNode(AGGREGATOR aggregator, FILTER_VALUE filterValue) {
		super(filterValue);
		this.aggregator = aggregator;
	}

	@Override
	public void collectRequiredTables(Set<Table> out) {
		aggregator.collectRequiredTables(out);
	}

	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		super.nextTable(ctx, currentTable);
		aggregator.nextTable(ctx, currentTable);
	}

	@Override
	public void nextBlock(Bucket bucket) {
		super.nextBlock(bucket);
		aggregator.nextBlock(bucket);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		aggregator.acceptEvent(bucket, event);
	}

	@Override
	public abstract boolean isContained();

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" +
			   "aggregator=" + aggregator +
			   ", filterValue=" + filterValue +
			   ')';
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return aggregator.isOfInterest(bucket);
	}
}
