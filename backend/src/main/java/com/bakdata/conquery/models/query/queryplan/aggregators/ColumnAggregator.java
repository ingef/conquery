package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.types.ColumnStore;

/**
 * Base class for aggregators acting on columns.
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
			ColumnStore type = bucket.getImp().getColumns()[column.getPosition()].getType();
			if (type.getNullLines() != type.getLines())
				return true;
		}

		return false;
	}
}
