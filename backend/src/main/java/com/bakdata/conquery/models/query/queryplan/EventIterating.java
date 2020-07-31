package com.bakdata.conquery.models.query.queryplan;

import java.util.HashSet;
import java.util.Set;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;

/***
 * Any object that iterates events in the QueryEngine.
 *
 * Methods are invoked in the following order:
 * 	- {@link EventIterating::isOfInterest(Entity)}
 * 	- {@link EventIterating::nextTable}
 * 	- {@link EventIterating::isOfInterest(Bucket)}
 * 	- {@link EventIterating::nextBlock}
 * 	- {@link EventIterating::acceptEvent}
 *
 * 	none of the isOfInterest methods may 
 */
public interface EventIterating {
	
	default void collectRequiredTables(Set<TableId> requiredTables) {}
	
	default Set<TableId> collectRequiredTables() {
		HashSet<TableId> out = new HashSet<>();
		this.collectRequiredTables(out);
		return out;
	}
	
	default void nextTable(QueryExecutionContext ctx, TableId currentTable) {}
	
	default void nextBlock(Bucket bucket) {}

	void acceptEvent(Bucket bucket, int event);


	default boolean isOfInterest(Bucket bucket){ return true; }
	
	default boolean isOfInterest(Entity entity){ return true; }
}
