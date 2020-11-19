package com.bakdata.conquery.models.query.queryplan.aggregators;

import java.util.Set;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.types.CType;

/**
 * Base class for aggregators acting on columns.
 */
public abstract class ColumnAggregator<T> implements Aggregator<T> {

	private boolean hit = false;
	protected T unhitDefault = null;

	@Override
	public void collectRequiredTables(Set<TableId> out) {
		for (Column column : getRequiredColumns()) {
			out.add(column.getTable().getId());
		}
	}

	public abstract Column[] getRequiredColumns();

	/**
	 * @implNote A {@link ColumnAggregator} must call {@link ColumnAggregator#setHit()} when it aggregates an event.
	 */
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
			CType type = bucket.getImp().getColumns()[column.getPosition()].getType();
			if (type.getNullLines() != type.getLines())
				return true;
		}

		return false;
	}
	
	public boolean isHit() {
		return hit;
	}
	
	/**
	 * Is called by the Aggregator to signal that it accepted an event.
	 * Hit should never be unset, which is why it is private.
	 */
	protected void setHit() {
		hit = true;
	}
	
	@Override
	public final T getAggregationResult() {
		return hit? doGetAggregationResult() : unhitDefault;
	}
	
	/**
	 * Is only called, when the {@link ColumnAggregator} signaled a hit when it accepted events.
	 * @see ColumnAggregator#acceptEvent(Bucket, int)
	 * @return
	 */
	abstract protected T doGetAggregationResult();
}
