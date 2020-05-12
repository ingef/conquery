package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.specific.AStringType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * This class is able to execute a typical ConceptQueryPlan, but will create
 * one result per distinct value in a secondaryId Column.
 */
@RequiredArgsConstructor
@Getter @Setter
public class SecondaryIdQueryPlan implements QueryPlan {

	private final ConceptQueryPlan query;
	private final SecondaryId secondaryId;
	private Map<String, ConceptQueryPlan> childPerKey = new HashMap<>();
	
	/**
	 * selects the right column for the given secondaryId from a table
	 */
	private Column findSecondaryIdColumn(Table table) {
		for(var col:table.getColumns()) {
			if(secondaryId.equals(col.getSecondaryId())) {
				return col;
			}
		}
		
		return null;
	}

	/**
	 * if a new distinct secondaryId was found we create a new clone of the ConceptQueryPlan
	 * and bring it up to speed
	 */
	private ConceptQueryPlan createChild(Object key, Column secondaryIdColumn, QueryExecutionContext currentContext, Bucket currentBucket) {
		ConceptQueryPlan plan = query.clone(new CloneContext(currentContext.getStorage()));
		plan.init(query.getEntity());
		plan.nextTable(currentContext, secondaryIdColumn.getTable());
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
		query.checkRequiredTables(ctx.getStorage());
		query.init(entity);
		if (query.getRequiredTables().isEmpty()) {
			return EntityResult.notContained();
		}

		List<Table> tablesWithoutSecondary = new ArrayList<>();
		//first execute only tables with secondaryIds
		for(Table currentTable : query.getRequiredTables()) {
			Column secondaryIdColumn = findSecondaryIdColumn(currentTable);
			if(secondaryIdColumn != null) {
				execute(ctx, entity, secondaryIdColumn);
			}
			else {
				tablesWithoutSecondary.add(currentTable);
			}
		}
		//afterwards the remaining tables, since we now spawned all children
		for(Table currentTable : tablesWithoutSecondary) {
			execute(ctx, entity, currentTable);
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

	private void execute(QueryExecutionContext ctx, Entity entity, Column secondaryIdColumn) {
		Table currentTable = secondaryIdColumn.getTable();
		nextTable(ctx, currentTable);
		for(Bucket bucket : entity.getBucket(currentTable.getId())) {
			int localEntity = bucket.toLocal(entity.getId());
			AStringType<?> secondaryIdType = (AStringType<?>)secondaryIdColumn.getTypeFor(bucket);
			nextBlock(bucket);
			if(bucket.containsLocalEntity(localEntity)) {
				if(isOfInterest(bucket)) {
					int start = bucket.getFirstEventOfLocal(localEntity);
					int end = bucket.getLastEventOfLocal(localEntity);
					for(int event = start; event < end ; event++) {
						//we ignore events with no value in the secondaryIdColumn
						if(bucket.has(event, secondaryIdColumn)) {
							String key = secondaryIdType.getElement(bucket.getString(event, secondaryIdColumn));
							childPerKey
								.computeIfAbsent(key, k->this.createChild(k, secondaryIdColumn, ctx, bucket))
								.nextEvent(bucket, event);
						}
					}
				}
			}
		}
	}
	
	private void execute(QueryExecutionContext ctx, Entity entity, Table currentTable) {
		nextTable(ctx, currentTable);
		for(Bucket bucket : entity.getBucket(currentTable.getId())) {
			int localEntity = bucket.toLocal(entity.getId());
			nextBlock(bucket);
			if(bucket.containsLocalEntity(localEntity)) {
				if(isOfInterest(bucket)) {
					int start = bucket.getFirstEventOfLocal(localEntity);
					int end = bucket.getLastEventOfLocal(localEntity);
					for(int event = start; event < end ; event++) {
						for(ConceptQueryPlan child : childPerKey.values()) {
							child.nextEvent(bucket, event);
						}
					}
				}
			}
		}
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

	private void nextTable(QueryExecutionContext ctx, Table currentTable) {
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
