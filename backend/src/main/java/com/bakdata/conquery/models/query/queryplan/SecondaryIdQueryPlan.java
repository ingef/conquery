package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.apiv1.query.concept.specific.CQConcept;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import com.bakdata.conquery.util.QueryUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

/**
 * This class is able to execute a typical ConceptQueryPlan, but will create
 * one result per distinct value in a {@link SecondaryIdDescriptionId} Column.
 *
 * @implNote This class will first execute the Query on all Tables carrying the selected {@link SecondaryIdDescriptionId}. Which will then be joined with all Tables that don't have a {@link SecondaryIdDescriptionId}, or are explicitly excluded (via {@link CQConcept#isExcludeFromSecondaryId()}.
 * <p>
 * This Query likely uses a lot of memory!
 */
@RequiredArgsConstructor
@Getter
@Setter
@ToString
public class SecondaryIdQueryPlan implements QueryPlan<MultilineEntityResult> {

	public static final int VALIDITY_DATE_POSITION = ConceptQueryPlan.VALIDITY_DATE_POSITION + 1;
	private final ConceptQuery query;
	private final QueryPlanContext queryPlanContext;

	private final SecondaryIdDescription secondaryId;

	private final Set<Column> tablesWithSecondaryId;
	private final Set<Table> tablesWithoutSecondaryId;

	private final ConceptQueryPlan queryPlan;


	private final Map<String, ConceptQueryPlan> childPerKey = new HashMap<>();
	/**
	 * This helps us avoid allocations, instead allowing us to reuse the queries.
	 */
	@Getter(AccessLevel.NONE)
	private final Queue<ConceptQueryPlan> childPlanReusePool = new LinkedList<>();

	/**
	 * This is the same execution as a typical ConceptQueryPlan. The difference
	 * is that this method will create a new cloned child for each distinct
	 * secondaryId it encounters during iteration.
	 *
	 * @return
	 */
	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {

		if (!queryPlan.isOfInterest(entity)) {
			return Optional.empty();
		}

		// First execute only tables with secondaryIds, creating all sub-queries
		for (Column entry : tablesWithSecondaryId) {
			executeQueriesWithSecondaryId(ctx, entity, entry);
		}
		// Afterwards the remaining tables, since we now spawned all children
		for (Table currentTable : tablesWithoutSecondaryId) {
			executeQueriesWithoutSecondaryId(ctx, entity, currentTable);
		}

		// Do a last run with the ALL_IDS Table to trigger ExternalNodes
		executeQueriesWithoutSecondaryId(ctx, entity, ctx.getStorage().getDataset().getAllIdsTable());

		return createResult(entity);
	}

	private void executeQueriesWithSecondaryId(QueryExecutionContext ctx, Entity entity, Column secondaryIdColumnId) {

		final QueryExecutionContext ctxWithPhase = ctx.withActiveSecondaryId(getSecondaryId());

		final Table currentTable = secondaryIdColumnId.getTable();

		nextTable(ctxWithPhase, currentTable);

		final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

		for (Bucket bucket : tableBuckets) {
			final int entityId = entity.getId();

			nextBlock(bucket);

			if (!bucket.containsEntity(entityId)) {
				continue;
			}

			if (!isOfInterest(bucket)) {
				continue;
			}

			final int start = bucket.getEntityStart(entityId);
			final int end = bucket.getEntityEnd(entityId);

			for (int event = start; event < end; event++) {
				//we ignore events with no value in the secondaryIdColumn
				if (!bucket.has(event, secondaryIdColumnId)) {
					continue;
				}

				final String key = ((String) bucket.createScriptValue(event, secondaryIdColumnId));

				if (childPerKey.containsKey(key)) {
					final ConceptQueryPlan plan = childPerKey.get(key);
					plan.nextEvent(bucket, event);
				}
				else {
					final ConceptQueryPlan plan = createChild(ctxWithPhase, bucket);
					final boolean consumed = plan.nextEvent(bucket, event);

					if (consumed) {
						childPerKey.put(key, plan);
					}
					else {
						childPlanReusePool.add(plan);
					}
				}
			}
		}
	}

	private void executeQueriesWithoutSecondaryId(QueryExecutionContext ctx, Entity entity, Table currentTable) {

		nextTable(ctx, currentTable);

		final List<Bucket> tableBuckets = ctx.getBucketManager().getEntityBucketsForTable(entity, currentTable);

		for (Bucket bucket : tableBuckets) {
			final int entityId = entity.getId();
			nextBlock(bucket);
			if (!bucket.containsEntity(entityId) || !isOfInterest(bucket)) {
				continue;
			}

			final int start = bucket.getEntityStart(entityId);
			final int end = bucket.getEntityEnd(entityId);

			for (int event = start; event < end; event++) {
				for (ConceptQueryPlan child : childPerKey.values()) {
					child.nextEvent(bucket, event);
				}
			}
		}
	}

	/**
	 * For each secondaryId that is included, create a line, return {@link MultilineEntityResult} containing all results.
	 */
	private Optional<MultilineEntityResult> createResult(Entity entity) {
		final List<Object[]> result = new ArrayList<>(childPerKey.values().size());

		// Prepend the key (ie the actual SecondaryId) to the result.
		for (Map.Entry<String, ConceptQueryPlan> child : childPerKey.entrySet()) {
			if (!child.getValue().isContained()) {
				continue;
			}

			// Prepend SecondaryId to result-line.
			result.add(ArrayUtils.insert(0, child.getValue().createResult().getValues(), child.getKey()));
		}

		if (result.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), result));
	}

	private void nextTable(QueryExecutionContext ctx, Table currentTable) {
		queryPlan.nextTable(ctx, currentTable);
		for (ConceptQueryPlan c : childPerKey.values()) {
			final QueryExecutionContext context = QueryUtils.determineDateAggregatorForContext(ctx, c::getValidityDateAggregator);
			c.nextTable(context, currentTable);
		}
	}

	private void nextBlock(Bucket bucket) {
		queryPlan.nextBlock(bucket);
		for (ConceptQueryPlan c : childPerKey.values()) {
			c.nextBlock(bucket);
		}
	}

	private boolean isOfInterest(Bucket bucket) {
		return queryPlan.isOfInterest(bucket);
	}

	/**
	 * if a new distinct secondaryId was found we create a new clone of the ConceptQueryPlan
	 * and bring it up to speed
	 */
	private ConceptQueryPlan createChild(QueryExecutionContext currentContext, Bucket currentBucket) {

		ConceptQueryPlan plan = childPlanReusePool.poll();

		// Try to reuse old child plan first before allocating new ones
		if (plan == null) {
			plan = query.createQueryPlan(queryPlanContext.withSelectedSecondaryId(secondaryId));
		}

		final QueryExecutionContext context = QueryUtils.determineDateAggregatorForContext(currentContext, plan::getValidityDateAggregator);

		plan.init(context, queryPlan.getEntity());
		plan.nextTable(context, currentBucket.getTable());
		plan.isOfInterest(currentBucket);
		plan.nextBlock(currentBucket);

		return plan;
	}

	@Override
	public void init(QueryExecutionContext ctx, Entity entity) {
		queryPlan.init(ctx, entity);

		// Dump the created children into reuse-pool
		childPlanReusePool.addAll(childPerKey.values());

		childPerKey.clear();
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return queryPlan.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		if (!queryPlan.isAggregateValidityDates()) {
			return Optional.empty();
		}

		final DateAggregator agg = new DateAggregator(DateAggregationAction.MERGE);
		childPerKey.values().forEach(c -> c.getValidityDateAggregator().ifPresent(agg::register));

		return agg.hasChildren() ? Optional.of(agg) : Optional.empty();
	}
}
