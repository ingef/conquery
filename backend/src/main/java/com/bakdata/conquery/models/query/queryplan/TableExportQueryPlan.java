package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The QueryPlan creates a full dump of the given table within a certain
 * date range.
 */
@RequiredArgsConstructor
public class TableExportQueryPlan implements QueryPlan {

	private final QueryPlan subPlan;
	private final CDateRange dateRange;
	private final List<TableExportDescription> tables;
	private final int totalColumns;

	@Override
	public QueryPlan clone(CloneContext ctx) {
		return new TableExportQueryPlan(subPlan.clone(ctx), dateRange, tables, totalColumns);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return subPlan.isOfInterest(entity);
	}

	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		EntityResult result = subPlan.execute(ctx, entity);

		if (!result.isContained()) {
			return result;
		}

		if (tables.isEmpty()) {
			return EntityResult.notContained();
		}


		List<Object[]> results = new ArrayList<>();
		for (TableExportDescription exportDescription : tables) {


			for (Bucket bucket : ctx.getEntityBucketsForTable(entity, exportDescription.getTable().getId())) {

				int entityId = entity.getId();

				if (!bucket.containsEntity(entityId)) {
					continue;
				}

				int start = bucket.getEntityStart(entityId);
				int end = bucket.getEntityEnd(entityId);

				for (int event = start; event < end; event++) {

					// Export Full-table if it has no validity date.
					if (exportDescription.getValidityDateColumn() != null && !bucket.eventIsContainedIn(event, exportDescription.getValidityDateColumn(), CDateSet.create(dateRange))) {
						continue;
					}

					Object[] entry = new Object[totalColumns];
					for (int col = 0; col < exportDescription.getTable().getColumns().length; col++) {
						final Column column = exportDescription.getTable().getColumns()[col];

						if (!bucket.has(event, column)) {
							continue;
						}

						ColumnStore type = bucket.getStore(column);

						// depending on context use pretty printing or script value
						entry[exportDescription.getColumnOffset() + col] = ctx.isPrettyPrint()
																		   ? bucket.createPrintValue(event, column)
																		   : bucket.createScriptValue(event, column);
					}

					results.add(entry);
				}
			}
		}

		return EntityResult.multilineOf(
				entity.getId(),
				results
		);
	}

	@RequiredArgsConstructor
	@Getter
	public static class TableExportDescription {
		private final Table table;
		@Nullable
		private final Column validityDateColumn;
		private final int columnOffset;
	}
}
