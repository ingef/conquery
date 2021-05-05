package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;

/**
 * Base class for aggregators acting on columns.
 */
public abstract class ColumnAggregator<T> implements Aggregator<T> {

	@Override
	public void collectRequiredTables(Set<Table> out) {
		for (Column column : getRequiredColumns()) {
			out.add(column.getTable());
		}
	}

	public abstract Column[] getRequiredColumns();

	@Override
	public abstract void acceptEvent(Bucket bucket, int event);

	@Override
	public String toString(){
		return getClass().getSimpleName();
	}
	
	public ColumnAggregator<T> clone(CloneContext ctx) {
		return ctx.clone(this);
	}

	/**
	 * Skip all buckets where none of the required columns have values.
	 * @param bucket
	 * @return
	 */
	@Override
	public boolean isOfInterest(Bucket bucket) {
		for (Column column : getRequiredColumns()) {
			if (!bucket.getStores()[column.getPosition()].isEmpty()) {
				return true;
			}
		}

		return false;
	}

}
