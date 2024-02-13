package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;

/**
 * Base class for aggregators acting on columns.
 */
public abstract class ColumnAggregator<T> extends Aggregator<T> {

	@Override
	public void collectRequiredTables(Set<Table> out) {
		for (Column column : getRequiredColumns()) {
			out.add(column.getTable());
		}
	}

	public abstract List<Column> getRequiredColumns();

	@Override
	public abstract void consumeEvent(Bucket bucket, int event);

	/**
	 * Skip all buckets where none of the required columns have values.
	 *
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

	@Override
	public String toString() {
		return "ColumnAggregator(column=" + getRequiredColumns() + ')';
	}

}
