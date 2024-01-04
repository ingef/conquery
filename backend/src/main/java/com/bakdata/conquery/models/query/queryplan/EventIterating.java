package com.bakdata.conquery.models.query.queryplan;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

/**
 * Implementing classes will be called in QueryEngine for evaluation.
 * <p>
 * Order of execution should be
 * 1) {@link EventIterating#init(Entity, QueryExecutionContext)} (once per Entity): resetting the {@link QueryPlan} for evaluation.
 * 2) {@link EventIterating#isOfInterest(Entity)} (once per Entity): Checking if the Entity has interesting data for the {@link EventIterating}
 * 3) {@link EventIterating#nextTable(QueryExecutionContext, Table)} (once per Table): Initializing the Event Iterating for the Table
 * 4) {@link EventIterating#isOfInterest(Bucket)}  (once per Bucket): Check if Bucket contains relevant information for this {@link EventIterating}
 * 5) {@link EventIterating#nextBlock(Bucket)}  (once per Bucket): Initialize the {@link EventIterating} for evaluation of the {@link Bucket}, for example prefetching {@link com.bakdata.conquery.models.events.CBlock}s
 * 6) {@link EventIterating#acceptEvent(Bucket, int)}  (per Event): Evaluation of the {@link QueryPlan} for this {@link EventIterating}
 * <p>
 */
public abstract class EventIterating {

	public void collectRequiredTables(Set<Table> requiredTables) {
	}

	public Set<Table> collectRequiredTables() {
		Set<Table> out = new HashSet<>();
		collectRequiredTables(out);
		return out;
	}

	/**
	 * Completely reset this object for reuse.
	 */
	public abstract void init(Entity entity, QueryExecutionContext context);

	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
	}

	public void nextBlock(Bucket bucket) {
	}

	public abstract boolean acceptEvent(Bucket bucket, int event);


	public boolean isOfInterest(Bucket bucket) {
		return true;
	}

	public boolean isOfInterest(Entity entity) {
		return true;
	}
}
