package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.specific.AStringType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class is able to execute a typical ConceptQueryPlan, but will create
 * one result per distinct value in a secondaryId Column.
 */
@RequiredArgsConstructor
@Getter @Setter
public class SecondaryIdQueryPlan implements QueryPlan {

	private final ConceptQueryPlan query;
	private final SecondaryId secondaryId;
	private Column currentSecondaryIdColumn;
	private Map<String, ConceptQueryPlan> childPerKey = new HashMap<>();
	
	/**
	 * selects the right column for the given secondaryId from a table
	 */
	private Column findSecondaryIdColumn(TableId tableId, NamespacedStorage storage) {
		final Table table = storage.getDataset().getTables().getOrFail(tableId);

		for (Column col : table.getColumns()) {
			if (!secondaryId.equals(col.getSecondaryId())) {
				continue;
			}

			return col;
		}
		
		throw new IllegalStateException(String.format("Table[%s] should not appear in a query about SecondaryId[%s]", table, secondaryId));
	}

	/**
	 * if a new distinct secondaryId was found we create a new clone of the ConceptQueryPlan
	 * and bring it up to speed
	 */
	private ConceptQueryPlan createChild(Object key, QueryExecutionContext currentContext, Bucket currentBucket) {
		ConceptQueryPlan plan = query.clone(new CloneContext(currentContext.getStorage()));
		plan.init(query.getEntity());
		plan.nextTable(currentContext, currentSecondaryIdColumn.getId().getTable());
		plan.isOfInterest(currentBucket);
		plan.nextBlock(currentBucket);
		return plan;
	}
	
	/**
	 * This is the same execution as a typical ConceptQueryPlan. The difference
	 * is that this method will create a new cloned child for each distinct
	 * secondaryId it encounters during iteration.
	 */
	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		if(!query.isOfInterest(entity)){
			return EntityResult.notContained();
		}

		query.checkRequiredTables(ctx.getStorage());
		query.init(entity);

    if (query.getRequiredTables().get().isEmpty()) {
			return EntityResult.notContained();
		}

		for(TableId currentTable : query.getRequiredTables().get()) {

			currentSecondaryIdColumn = findSecondaryIdColumn(currentTable, ctx.getStorage());
			nextTable(ctx, currentTable);

			for(Bucket bucket : entity.getBucket(currentTable)) {
				int localEntity = bucket.toLocal(entity.getId());
				AStringType<?> secondaryIdType = (AStringType<?>)currentSecondaryIdColumn.getTypeFor(bucket);
				nextBlock(bucket);
				if (!bucket.containsLocalEntity(localEntity)) {
					continue;
				}

				if (!isOfInterest(bucket)) {
					continue;
				}

				int start = bucket.getFirstEventOfLocal(localEntity);
				int end = bucket.getLastEventOfLocal(localEntity);
				for(int event = start; event < end ; event++) {
					//we ignore events with no value in the secondaryIdColumn
					if (!bucket.has(event, currentSecondaryIdColumn)) {
						continue;
					}

					String key = secondaryIdType.getElement(bucket.getString(event, currentSecondaryIdColumn));
					childPerKey.computeIfAbsent(key, k -> this.createChild(k, ctx, bucket))
							   .nextEvent(bucket, event);
				}
			}
		}
		
		
		var result = new ArrayList<Object[]>(childPerKey.values().size());
		for(var child:childPerKey.entrySet()) {
			if(child.getValue().isContained()) {
				result.add(ArrayUtils.insert(0, child.getValue().result().getValues(), child.getKey()));
			}
		}
		if(result.isEmpty()) {
			return EntityResult.notContained();
		}
		return EntityResult.multilineOf(entity.getId(), result);
	}

	private boolean isOfInterest(Bucket bucket) {
		for(ConceptQueryPlan c:childPerKey.values()) {
			c.isOfInterest(bucket);
		}
		return query.isOfInterest(bucket);
	}

	private void nextBlock(Bucket bucket) {
		query.nextBlock(bucket);
		for(ConceptQueryPlan c:childPerKey.values()) {
			c.nextBlock(bucket);
		}
	}

	private void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		query.nextTable(ctx, currentTable);
		for(ConceptQueryPlan c:childPerKey.values()) {
			c.nextTable(ctx, currentTable);
		}
	}

	@Override
	public QueryPlan clone(CloneContext ctx) {
		return new SecondaryIdQueryPlan(query.clone(ctx), secondaryId);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}
}
