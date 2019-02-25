package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;

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
			QueryPlan queryPlan = this.plan.clone();
			queryPlan.init(entity);
			
			if (requiredTables.isEmpty()) {
				return EntityResult.notContained();
			}

			for(Table currentTable : requiredTables) {
				queryPlan.nextTable(ctx, currentTable);
				for(Block block : entity.getBlocks().get(currentTable)) {
					queryPlan.nextBlock(block);
					for(int event = block.size()-1; event >= 0 ; event--) {
						if(!queryPlan.aggregate(block, event)) {
							return EntityResult.notContained();
						}
					}
				}
			}
	
			return queryPlan.createResult();
		}
		catch(Exception e) {
			return EntityResult.failed(entity.getId(), e);
		}
	}
}
