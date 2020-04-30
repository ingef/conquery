package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.types.CType;

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
	private final List<TableExportConnector> tables;
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
		
		if(!result.isContained()) {
			return result;
		}
		
		if (tables.isEmpty()) {
			return EntityResult.notContained();
		}

		
		List<Object[]> results = new ArrayList<>();
		for(TableExportConnector tec : tables) {
			for(Bucket bucket : entity.getBucket(tec.getTable().getId())) {
				int localEntity = bucket.toLocal(entity.getId());
				if(bucket.containsLocalEntity(localEntity)) {
					int start = bucket.getFirstEventOfLocal(localEntity);
					int end = bucket.getLastEventOfLocal(localEntity);
					for(int event = start; event < end ; event++) {
						if (bucket.eventIsContainedIn(event, tec.getValidityDateColumn(), dateRange)) {
							Object[] entry = new Object[totalColumns];
							for(int col = 0; col < tec.getTable().getColumns().length; col++) {
								CType type = tec.getTable().getColumns()[col].getTypeFor(bucket);
	
								// depending on context use pretty printing or script value
								entry[col+tec.getColumnOffset()] = ctx.isPrettyPrint()
									? type.createPrintValue(bucket.getRaw(event, col))
									: type.createScriptValue(bucket.getRaw(event, col));
							}
							results.add(entry);
						}
					}
				}
			}
		}
		//pivot the last column
		return EntityResult.multilineOf(
			result.asContained().getEntityId(),
			results
		);
	}
	
	@RequiredArgsConstructor
	@Getter
	public static class TableExportConnector {
		private final Table table;
		private final Column validityDateColumn;
		private final int columnOffset;
	}
}
