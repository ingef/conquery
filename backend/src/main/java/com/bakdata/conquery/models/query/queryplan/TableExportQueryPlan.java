package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bakdata.conquery.apiv1.query.concept.filter.CQTable;
import com.bakdata.conquery.apiv1.query.concept.filter.ValidityDateContainer;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * The QueryPlan creates a full dump of the given table within a certain
 * date range.
 */
@RequiredArgsConstructor
@ToString
public class TableExportQueryPlan implements QueryPlan<MultilineEntityResult> {

	private final QueryPlan<? extends EntityResult> subPlan;
	private final CDateSet dateRange;
	private final Map<CQTable, QPNode> tables;

	@ToString.Exclude
	private final Map<Column, Integer> positions;

	public static Column findValidityDateColumn(Connector connector, ValidityDateContainer dateColumn) {
		// if no dateColumn is provided, we use the default instead which is always the first one.
		// Set to null if none-available in the connector.
		if (dateColumn != null) {
			return dateColumn.getValue().getColumn();
		}

		if (!connector.getValidityDates().isEmpty()) {
			return connector.getValidityDates().get(0).getColumn();
		}

		return null;
	}

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

		for (Map.Entry<CQTable, QPNode> entry : tables.entrySet()) {

			final CQTable cqTable = entry.getKey();
			final Column validityDateColumn = findValidityDateColumn(cqTable.getConnector(), cqTable.getDateColumn());
			final QPNode query = entry.getValue();

			for (Bucket bucket : ctx.getEntityBucketsForTable(entity, cqTable.getConnector().getTable())) {

				int entityId = entity.getId();

				if (!bucket.containsEntity(entityId)) {
					continue;
				}

				final int start = bucket.getEntityStart(entityId);
				final int end = bucket.getEntityEnd(entityId);

				for (int event = start; event < end; event++) {

					// Export Full-table if it has no validity date.
					if (validityDateColumn != null
						&& !bucket.eventIsContainedIn(event, validityDateColumn, dateRange)) {
						continue;
					}

					if (!isRowIncluded(ctx, entity, query, bucket, event)) {
						continue;
					}

					final Object[] resultRow = collectRow(totalColumns, cqTable, bucket, event, validityDateColumn);

					results.add(resultRow);
				}
			}
		}

		return Optional.of(new MultilineEntityResult(entity.getId(), results));
	}

	private boolean isRowIncluded(QueryExecutionContext ctx, Entity entity, QPNode query, Bucket bucket, int event) {
		query.init(entity, ctx);

		if (!query.isOfInterest(entity)) {
			return false;
		}

		query.nextTable(ctx, bucket.getTable());
		query.nextBlock(bucket);

		if (!query.isOfInterest(bucket)) {
			return false;
		}

		query.acceptEvent(bucket, event);

		return query.isContained();
	}

	private Object[] collectRow(int totalColumns, CQTable exportDescription, Bucket bucket, int event, Column validityDateColumn) {

		final Object[] entry = new Object[totalColumns];
		entry[1] = exportDescription.getConnector().getTable().getLabel();

		for (Column column : exportDescription.getConnector().getTable().getColumns()) {

			if (!bucket.has(event, column)) {
				continue;
			}

			if (column.equals(validityDateColumn)) {
				entry[0] = List.of(bucket.getAsDateRange(event, column));
			}
			else {
				entry[positions.get(column)] = bucket.createScriptValue(event, column);
			}
		}
		return entry;
	}

}
