package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * The QueryPlan creates a full dump of the given table within a certain
 * date range.
 */
@RequiredArgsConstructor
@ToString
public class TableExportQueryPlan implements QueryPlan<MultilineEntityResult> {

	/**
	 * Query used to export tables filtered by entity. If the subPlan evaluates as contained for an entity, the corresponding table contents will be exported.
	 */
	private final QueryPlan<? extends EntityResult> subPlan;

	//TODO FK: Implement this as ValidityDateNode in QPNode instead.
	private final CDateSet dateRange;
	private final Map<CQTable, QPNode> tables;

	@ToString.Exclude
	private final Map<Column, Integer> positions;

	/**
	 * If true, Connector {@link Column}s will be output raw.
	 * If false, the {@link com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId} will be output.
	 */
	@Getter
	private final boolean rawConceptValues;


	@Override
	public boolean isOfInterest(Entity entity) {
		return subPlan.isOfInterest(entity);
	}

	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		// TODO create a fake aggregator and feed it inside the loop, return it here.
		return Optional.empty();
	}

	@Override
	public void init(QueryExecutionContext ctxt, Entity entity) {
		subPlan.init(ctxt, entity);
	}

	@Override
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {
		Optional<? extends EntityResult> result = subPlan.execute(ctx, entity);


		if (result.isEmpty() || tables.isEmpty()) {
			return Optional.empty();
		}

		final List<Object[]> results = new ArrayList<>();

		final int totalColumns = positions.values().stream().mapToInt(i -> i).max().getAsInt() + 1;
		final int entityId = entity.getId();

		for (Map.Entry<CQTable, QPNode> entry : tables.entrySet()) {

			final CQTable cqTable = entry.getKey();
			final ValidityDate validityDate = cqTable.findValidityDate();
			final QPNode query = entry.getValue();
			final Map<Bucket, CBlock> cblocks = ctx.getBucketManager().getEntityCBlocksForConnector(entity, cqTable.getConnector());

			for (Bucket bucket : ctx.getEntityBucketsForTable(entity, cqTable.getConnector().getTable())) {

				if (!shouldEvaluateBucket(query, bucket, entity, ctx)) {
					continue;
				}

				final int start = bucket.getEntityStart(entityId);
				final int end = bucket.getEntityEnd(entityId);

				for (int event = start; event < end; event++) {

					if (validityDate != null
						&& !bucket.eventIsContainedIn(event, validityDate, dateRange)) {
						continue;
					}

					if (!isRowIncluded(query, bucket, entity, event, ctx)) {
						continue;
					}

					final Object[] resultRow = collectRow(totalColumns, cqTable, bucket, event, validityDate, cblocks.get(bucket));

					results.add(resultRow);
				}
			}
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), results));
	}

	/**
	 * Test if the Bucket should even be evaluated for the {@link QPNode}.
	 * <p>
	 * Note that we are cramming a few things together at once, but it's probably not such a huge waste of compute time since there are only a few Buckets per Entity.
	 */
	private boolean shouldEvaluateBucket(QPNode query, Bucket bucket, Entity entity, QueryExecutionContext ctx) {
		query.init(entity, ctx);

		if (!query.isOfInterest(entity)) {
			return false;
		}

		query.nextTable(ctx, bucket.getTable());
		query.nextBlock(bucket);

		return query.isOfInterest(bucket);
	}

	/**
	 * We execute the QPNode on a single row as though it was a whole query. To check if the event-row should be included.
	 */
	private boolean isRowIncluded(QPNode query, Bucket bucket, Entity entity, int event, QueryExecutionContext ctx) {
		query.init(entity, ctx);

		query.nextTable(ctx, bucket.getTable());
		query.nextBlock(bucket);

		query.acceptEvent(bucket, event);

		return query.isContained();
	}

	private Object[] collectRow(int totalColumns, CQTable exportDescription, Bucket bucket, int event, ValidityDate validityDate, CBlock cblock) {

		final Object[] entry = new Object[totalColumns];
		entry[1] = exportDescription.getConnector().getTable().getLabel();
		entry[0] = List.of(validityDate.getValidityDate(event, bucket));

		for (Column column : exportDescription.getConnector().getTable().getColumns()) {

			if (!bucket.has(event, column)) {
				continue;
			}

			if (positions.containsKey(column)) {
				continue;
			}

			final int position = positions.get(column);


			if (!rawConceptValues && column.equals(exportDescription.getConnector().getColumn())) {
				entry[position] = cblock.getMostSpecificChildLocalId(event);
				continue;
			}

			entry[position] = bucket.createScriptValue(event, column);
		}
		return entry;
	}

}
