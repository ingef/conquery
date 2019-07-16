package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Base class for aggregators acting on columns.
 * @param <T>
 */
public abstract class ColumnAggregator<T> implements Aggregator<T> {

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		for (Column column : getRequiredColumns()) {
			out.add(column.getTable().getId());
		}
	}

	public abstract Column[] getRequiredColumns();

	@Override
	public abstract void aggregateEvent(Bucket bucket, int event);

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}
	
	@Override
	public ColumnAggregator<T> clone(CloneContext ctx) {
		return ctx.clone(this);
	}
}
