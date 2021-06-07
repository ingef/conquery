package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.specific.FiltersNode;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineEntityResult;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * The QueryPlan creates a full dump of the given table within a certain
 * date range.
 */
@RequiredArgsConstructor
public class TableExportQueryPlan implements QueryPlan<MultilineEntityResult> {

	private final QueryPlan<? extends EntityResult> subPlan;
	private final CDateRange dateRange;
	private final List<TableExportDescription> tables;
	private final Map<Column, Integer> positions;

	@Override
	public QueryPlan<MultilineEntityResult> clone(CloneContext ctx) {
		return new TableExportQueryPlan(subPlan.clone(ctx), dateRange, tables, positions);
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
	public Optional<MultilineEntityResult> execute(QueryExecutionContext ctx, Entity entity) {
		Optional<? extends EntityResult> result = subPlan.execute(ctx, entity);

		int entityId = entity.getId();

		if (result.isEmpty() || tables.isEmpty()) {
			return Optional.empty();
		}

		List<Object[]> results = new ArrayList<>();

		final int totalColumns = positions.values().stream().mapToInt(i -> i).max().getAsInt() + 1;

		for (TableExportDescription exportDescription : tables) {
			final FiltersNode filter = exportDescription.getFilter();

			final Table table = exportDescription.getTable();

			filter.init(entity, ctx);
			filter.nextTable(ctx, table);

			for (Bucket bucket : ctx.getEntityBucketsForTable(entity, table)) {

				filter.nextBlock(bucket);

				if (!bucket.containsEntity(entityId)) {
					continue;
				}


				int start = bucket.getEntityStart(entityId);
				int end = bucket.getEntityEnd(entityId);

				for (int event = start; event < end; event++) {

					// Export Full-table if it has no validity date.
					if (exportDescription.getValidityDateColumn() != null
						&& !bucket.eventIsContainedIn(event, exportDescription.getValidityDateColumn(), CDateSet.create(dateRange))) {
						continue;
					}

					if (!filter.checkEvent(bucket, event)) {
						continue;
					}


					Object[] entry = new Object[totalColumns];

					for (Column column : table.getColumns()) {

						if (!bucket.has(event, column)) {
							continue;
						}

						if (column.equals(exportDescription.getValidityDateColumn())) {
							entry[0] = List.of(bucket.getAsDateRange(event, column));
						}
						else {
							entry[positions.get(column)] = bucket.createScriptValue(event, column);
						}
					}

					results.add(entry);
				}
			}
		}

		if(results.isEmpty()){
			return Optional.empty();
		}

		return Optional.of(new MultilineEntityResult(
				entity.getId(),
				results
		));
	}

	@Data
	public static class TableExportDescription {
		private final Table table;
		@Nullable
		private final Column validityDateColumn;
		@NonNull
		private final FiltersNode filter;
	}
}
