package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.io.xodus.ModificationShieldedWorkerStorage;
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
@Getter
@Setter
public class SecondaryIdQueryPlan implements QueryPlan {

	private final ConceptQueryPlan query;
	private final SecondaryId secondaryId;
	private Map<String, ConceptQueryPlan> childPerKey = new HashMap<>();

	/**
	 * This is the same execution as a typical ConceptQueryPlan. The difference
	 * is that this method will create a new cloned child for each distinct
	 * secondaryId it encounters during iteration.
	 */
	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {


		if (query.getRequiredTables().get().isEmpty()) {
			return EntityResult.notContained();
		}

		query.checkRequiredTables(ctx.getStorage());
		query.init(entity, ctx);

		if (!query.isOfInterest(entity)) {
			return EntityResult.notContained();
		}

		List<TableId> tablesWithoutSecondary = new ArrayList<>();
		//first execute only tables with secondaryIds
		for (TableId currentTable : query.getRequiredTables().get()) {
			Column secondaryIdColumn = findSecondaryIdColumn(currentTable, ctx.getStorage());
			if (secondaryIdColumn != null) {
				executeQueriesWithSecondaryId(ctx, entity, secondaryIdColumn);
			}
			else {
				tablesWithoutSecondary.add(currentTable);
			}
		}
		//afterwards the remaining tables, since we now spawned all children
		for (TableId currentTable : tablesWithoutSecondary) {
			executeQueriesWithoutSecondaryId(ctx, entity, currentTable);
		}


		var result = new ArrayList<Object[]>(childPerKey.values().size());
		for (var child : childPerKey.entrySet()) {
			if (child.getValue().isContained()) {
				result.add(ArrayUtils.insert(0, child.getValue().result().getValues(), child.getKey()));
			}
		}
		if (result.isEmpty()) {
			return EntityResult.notContained();
		}
		return EntityResult.multilineOf(entity.getId(), result);
	}

	/**
	 * selects the right column for the given secondaryId from a table
	 */
	private Column findSecondaryIdColumn(TableId tableId, ModificationShieldedWorkerStorage storage) {
		final Table table = storage.getDataset().getTables().getOrFail(tableId);

		for (Column col : table.getColumns()) {
			if (!secondaryId.equals(col.getSecondaryId())) {
				continue;
			}

			return col;
		}

		return null;
	}

	private void executeQueriesWithSecondaryId(QueryExecutionContext ctx, Entity entity, Column secondaryIdColumn) {
		TableId currentTable = secondaryIdColumn.getTable().getId();
		nextTable(ctx, currentTable);

		final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

		for (Bucket bucket : tableBuckets) {
			int localEntity = bucket.toLocal(entity.getId());

			AStringType<?> secondaryIdType = (AStringType<?>) secondaryIdColumn.getTypeFor(bucket);

			nextBlock(bucket);

			if (!bucket.containsLocalEntity(localEntity) || !isOfInterest(bucket)) {
				continue;
			}

			int start = bucket.getFirstEventOfLocal(localEntity);
			int end = bucket.getLastEventOfLocal(localEntity);

			for (int event = start; event < end; event++) {
				//we ignore events with no value in the secondaryIdColumn
				if (!bucket.has(event, secondaryIdColumn)) {
					continue;
				}

				String key = secondaryIdType.getElement(bucket.getString(event, secondaryIdColumn));
				childPerKey.computeIfAbsent(key, k -> this.createChild(secondaryIdColumn, ctx, bucket))
						   .nextEvent(bucket, event);
			}
		}
	}

	private void executeQueriesWithoutSecondaryId(QueryExecutionContext ctx, Entity entity, TableId currentTable) {
		nextTable(ctx, currentTable);

		final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

		for (Bucket bucket : tableBuckets) {
			int localEntity = bucket.toLocal(entity.getId());
			nextBlock(bucket);
			if (!bucket.containsLocalEntity(localEntity) || !isOfInterest(bucket)) {
				continue;
			}

			int start = bucket.getFirstEventOfLocal(localEntity);
			int end = bucket.getLastEventOfLocal(localEntity);

			for (int event = start; event < end; event++) {
				for (ConceptQueryPlan child : childPerKey.values()) {
					child.nextEvent(bucket, event);
				}
			}
		}
	}

	private void nextTable(QueryExecutionContext ctx, TableId currentTable) {
		query.nextTable(ctx, currentTable);
		for (ConceptQueryPlan c : childPerKey.values()) {
			c.nextTable(ctx, currentTable);
		}
	}

	private void nextBlock(Bucket bucket) {
		query.nextBlock(bucket);
		for (ConceptQueryPlan c : childPerKey.values()) {
			c.nextBlock(bucket);
		}
	}

	private boolean isOfInterest(Bucket bucket) {
		return query.isOfInterest(bucket);
	}

	/**
	 * if a new distinct secondaryId was found we create a new clone of the ConceptQueryPlan
	 * and bring it up to speed
	 */
	private ConceptQueryPlan createChild(Column secondaryIdColumn, QueryExecutionContext currentContext, Bucket currentBucket) {

		ConceptQueryPlan plan = query.clone(new CloneContext(currentContext.getStorage()));

		plan.init(query.getEntity(), currentContext);
		plan.nextTable(currentContext, secondaryIdColumn.getId().getTable());
		plan.isOfInterest(currentBucket);
		plan.nextBlock(currentBucket);

		return plan;
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
