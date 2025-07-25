package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.TableExportQuery;
import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.ValidityDate;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.bakdata.conquery.models.identifiable.ids.specific.CBlockId;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

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
	private final Map<ColumnId, Integer> positions;

	/**
	 * If true, Connector {@link Column}s will be output raw.
	 * If false, the {@link com.bakdata.conquery.models.identifiable.ids.specific.ConceptElementId} will be output.
	 */
	@Getter
	private final boolean rawConceptValues;

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

		final int totalColumns = TableExportQuery.calculateWidth(positions);
		final String entityId = entity.getId();

		for (Map.Entry<CQTable, QPNode> entry : tables.entrySet()) {

			final CQTable cqTable = entry.getKey();
			final ValidityDate validityDate = cqTable.findValidityDate();
			final QPNode query = entry.getValue();
			final Map<BucketId, CBlockId> cblocks = ctx.getBucketManager().getEntityCBlocksForConnector(entity, cqTable.getConnector());
			final Connector connector = cqTable.getConnector().resolve();

			for (BucketId bucketId : ctx.getEntityBucketsForTable(entity, connector.resolveTableId())) {
				Bucket bucket = bucketId.resolve();
				CBlock cBlock = cblocks.get(bucketId).resolve();

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

					final Object[] resultRow = collectRow(totalColumns, cqTable, bucket, event, validityDate, cBlock);

					results.add(resultRow);
				}
			}
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), results));
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return subPlan.isOfInterest(entity);
	}

	@NotNull
	@Override
	public Optional<Aggregator<CDateSet>> getValidityDateAggregator() {
		// TODO create a fake aggregator and feed it inside the loop, return it here.
		return Optional.empty();
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

		query.nextTable(ctx, bucket.getTable().resolve());
		query.nextBlock(bucket);

		return query.isOfInterest(bucket);
	}

	/**
	 * We execute the QPNode on a single row as though it was a whole query. To check if the event-row should be included.
	 */
	private boolean isRowIncluded(QPNode query, Bucket bucket, Entity entity, int event, QueryExecutionContext ctx) {
		query.init(entity, ctx);

		query.nextTable(ctx, bucket.getTable().resolve());
		query.nextBlock(bucket);

		query.acceptEvent(bucket, event);

		return query.isContained();
	}

	private Object[] collectRow(int totalColumns, CQTable exportDescription, Bucket bucket, int event, ValidityDate validityDate, CBlock cblock) {

		final Object[] entry = new Object[totalColumns];

		final CDateRange date;

		if(validityDate != null && (date = validityDate.getValidityDate(event, bucket)) !=  null) {
			entry[0] = List.of(date);
		}

		final Connector connector = exportDescription.getConnector().resolve();
		entry[1] = connector.getResolvedTable().getLabel();

		for (Column column : connector.getResolvedTable().getColumns()) {
			final ColumnId columnId = column.getId();

			// ValidityDates are handled separately.
			if (validityDate != null && validityDate.containsColumn(columnId)) {
				continue;
			}

			if (!positions.containsKey(columnId)) {
				continue;
			}

			if (!bucket.has(event, column)) {
				continue;
			}

			final int position = positions.get(columnId);

			if (!rawConceptValues && columnId.equals(connector.getColumn())) {
				entry[position] = cblock.getMostSpecificChildLocalId(event);
				continue;
			}

			entry[position] = bucket.createScriptValue(event, column);
		}
		return entry;
	}

}
