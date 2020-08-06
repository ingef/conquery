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
 * 	@implNote none of the isOfInterest methods may maintain state.
 */
public interface EventIterating {

	/**
	 * Add all {@code TableId} necessary for processing of this object.
	 *
	 * @apiNote an object without required tables is always active (See {@link QPParentNode::nextTable}), but passive.
	 *
	 * @param requiredTables the set to inser the tables into.
	 */
	default void collectRequiredTables(Set<TableId> requiredTables) {}
	
	default Set<TableId> collectRequiredTables() {
		HashSet<TableId> out = new HashSet<>();
		this.collectRequiredTables(out);
		return out;
	}

	/**
	 * Indicate if this or any elements below this element are always active.
	 *
	 * @implNote This is used to have Aggregators without specific Tables/Columns that receive every entry irr
	 */
	default boolean isAlwaysActive(){
		return false;
	}

	/**
	 * Check if the Entity has relevant data. This method is used as early exit for queries to avoid unnecessary computation.
	 *
	 * @implNote you may want to create some indices in the Entity to facilitate this object, but consider the additional memory overhead first.
	 *
	 * @return false iff the object will never be included in a response of this object. True, if the object may be included in a response by this object.
	 */
	default boolean isOfInterest(Entity entity){ return true; }

	/**
	 * Prefetch data belonging to the nextTable. For example disable some logic based on the data in the table or updating dateRestrictions.
	 */
	default void nextTable(QueryExecutionContext ctx, TableId currentTable) {}

	/**
	 * Check if the Bucket will contain data relevant to this EventIterating.
	 *
	 * @return false iff the object will never be included in a response of this object. True, if the object may be included in a response by this object.
	 */
	default boolean isOfInterest(Bucket bucket){ return true; }

	/**
	 * Prefetch data relevant to the incoming Bucket, for example load string-value mappings of the bucket.
	 */
	default void nextBlock(Bucket bucket) {}

	/**
	 * Process the event of the Bucket. It is not guaranteed, that the bucket has associated data, so a {@link Bucket::has}, call is necessary.
	 *
	 * @param bucket the bucket containing the data.
	 * @param event the event to process.
	 */
	void acceptEvent(Bucket bucket, int event);

}
