package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.util.QueryUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class is able to execute a typical ConceptQueryPlan, but will create
 * one result per distinct value in a {@link SecondaryIdDescriptionId} Column.
 *
 * @implNote This class will first execute the Query on all Tables carrying the selected {@link SecondaryIdDescriptionId}. Which will then be joined with all Tables that don't have a {@link SecondaryIdDescriptionId}, or are explicitly excluded (via {@link CQConcept#isExcludeFromSecondaryIdQuery()}.
 *
 * This Query likely uses a lot of memory!
 */
@RequiredArgsConstructor
@Getter
@Setter
public class SecondaryIdQueryPlan implements QueryPlan<MultilineEntityResult> {

	public static final int VALIDITY_DATE_POSITION = ConceptQueryPlan.VALIDITY_DATE_POSITION + 1;
	private final ConceptQueryPlan query;
	private final SecondaryIdDescription secondaryId;

	private final Set<Column> tablesWithSecondaryId;
	private final Set<Table> tablesWithoutSecondaryId;

	private Map<String, ConceptQueryPlan> childPerKey = new HashMap<>();

	/**
	 * This is the same execution as a typical ConceptQueryPlan. The difference
	 * is that this method will create a new cloned child for each distinct
	 * secondaryId it encounters during iteration.
	 * @return
	 */
	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		if (query.getRequiredTables().get().isEmpty()) {
			return Optional.empty();
		}

		query.checkRequiredTables(ctx.getStorage());
		query.init(entity, ctx);

		if (!query.isOfInterest(entity)) {
			return Optional.empty();
		}

		//first execute only tables with secondaryIds, creating all sub-queries
		for (Column entry : tablesWithSecondaryId) {
			executeQueriesWithSecondaryId(ctx, entity, entry);
		}
		//afterwards the remaining tables, since we now spawned all children
		for (Table currentTable : tablesWithoutSecondaryId) {
			executeQueriesWithoutSecondaryId(ctx, entity, currentTable);
		}


		List<Object[]> result = new ArrayList<>(childPerKey.values().size());

		// Prepend the key (ie the actual SecondaryId) to the result.
		for (Map.Entry<String, ConceptQueryPlan> child : childPerKey.entrySet()) {
			if (!child.getValue().isContained()) {
				continue;
			}

			// Prepend SecondaryId to result-line.
			result.add(ArrayUtils.insert(0, child.getValue().result().getValues(), child.getKey()));
		}


		if (result.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), result));
	}


	private void executeQueriesWithSecondaryId(QueryExecutionContext ctx, Entity entity, Column secondaryIdColumnId) {

		QueryExecutionContext ctxWithPhase = ctx.withActiveSecondaryId(getSecondaryId());

		Table currentTable = secondaryIdColumnId.getTable();

		nextTable(ctxWithPhase, currentTable);

		final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

		for (Bucket bucket : tableBuckets) {
			int entityId = entity.getId();

			nextBlock(bucket);

			if (!bucket.containsEntity(entityId)) {
				continue;
			}

			if(!isOfInterest(bucket)){
				continue;
			}

			int start = bucket.getEntityStart(entityId);
			int end = bucket.getEntityEnd(entityId);

			for (int event = start; event < end; event++) {
				//we ignore events with no value in the secondaryIdColumn
				if (!bucket.has(event, secondaryIdColumnId)) {
					continue;
				}

				String key = ((String) bucket.createScriptValue(event, secondaryIdColumnId));
				final ConceptQueryPlan plan = childPerKey.computeIfAbsent(key, k -> this.createChild(secondaryIdColumnId, ctxWithPhase, bucket));
				plan.nextEvent(bucket, event);
			}
		}
	}

	private void executeQueriesWithoutSecondaryId(QueryExecutionContext ctx, Entity entity, Table currentTable) {

		nextTable(ctx, currentTable);

		final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

		for (Bucket bucket : tableBuckets) {
			int entityId = entity.getId();
			nextBlock(bucket);
			if (!bucket.containsEntity(entityId) || !isOfInterest(bucket)) {
				continue;
			}

			int start = bucket.getEntityStart(entityId);
			int end = bucket.getEntityEnd(entityId);

			for (int event = start; event < end; event++) {
				for (ConceptQueryPlan child : childPerKey.values()) {
					child.nextEvent(bucket, event);
				}
			}
		}
	}

	private void nextTable(QueryExecutionContext ctx, Table currentTable) {
		query.nextTable(ctx, currentTable);
		for (ConceptQueryPlan c : childPerKey.values()) {
			QueryExecutionContext context = QueryUtils.determineDateAggregatorForContext(ctx, c::getValidityDateAggregator);
			c.nextTable(context, currentTable);
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

		QueryExecutionContext context = QueryUtils.determineDateAggregatorForContext(currentContext, plan::getValidityDateAggregator);

		plan.init(query.getEntity(), context);
		plan.nextTable(context, secondaryIdColumn.getTable());
		plan.isOfInterest(currentBucket);
		plan.nextBlock(currentBucket);

		return plan;
	}

	@Override
	public QueryPlan clone(CloneContext ctx) {
		return new SecondaryIdQueryPlan(query.clone(ctx), secondaryId, tablesWithSecondaryId, tablesWithoutSecondaryId);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		if(!query.isAggregateValidityDates()) {
			return Optional.empty();
		}

		DateAggregator agg = new DateAggregator(DateAggregationAction.MERGE);
		childPerKey.values().forEach(c -> c.getValidityDateAggregator().ifPresent(agg::register));

		return agg.hasChildren() ? Optional.of(agg) : Optional.empty();
	}
}
