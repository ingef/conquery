package com.bakdata.conquery.models.query;

import java.util.Set;
import java.util.concurrent.Callable;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.google.common.primitives.Doubles;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryPart implements Callable<EntityResult> {

	private final QueryContext ctx;
	private final QueryPlan plan;
	private final Set<Table> requiredTables;
	private final Entity entity;
	
	@Override
	public EntityResult call() throws Exception {
		try {
			QueryPlan queryPlan = this.plan.createClone();
			queryPlan.init(entity);
			
			if (requiredTables.isEmpty()) {
				return EntityResult.notContained();
			}

			for(Table currentTable : requiredTables) {
				queryPlan.nextTable(ctx, currentTable);
				for(Block block : entity.getBlocks().get(currentTable)) {
					queryPlan.nextBlock(block);
					for(int event = block.size()-1; event >= 0 ; event--) {
						queryPlan.nextEvent(block, event);
					}
				}
			}
	
			//ugly workaround which we should find a fix for
			EntityResult result = queryPlan.createResult();
			if(result instanceof ContainedEntityResult) {
				((ContainedEntityResult) result).streamValues().forEach(row -> {
					for(int i=0;i<row.length;i++) {
						if(row[i] instanceof Double) {
							double v = (Double) row[i];
							if(Double.isInfinite(v) || Double.isNaN(v)) {
								row[i] = null;
							}
						}
					}
				});
			}
			
			return result;
		}
		catch(Exception e) {
			return EntityResult.failed(entity.getId(), e);
		}
	}
}
