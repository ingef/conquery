package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.concurrent.Callable;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.google.common.collect.Multiset;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueryPart implements Callable<EntityResult> {

	private final QueryContext ctx;
	private final QueryPlan plan;
	private final Entity entity;
	
	@Override
	public EntityResult call() throws Exception {
		try {
			QueryPlan queryPlan = this.plan.clone();
			QPNode root = queryPlan.getRoot();
			root.init(entity);
			Multiset<Table> requiredTables = root.collectRequiredTables();

			if (requiredTables.isEmpty()) {
				return EntityResult.notContained();
			}

			while(!requiredTables.isEmpty()) {
				Table currentTable = requiredTables.iterator().next();
				
				root.nextTable(ctx, currentTable);
				for(Block block : entity.getBlocks().get(currentTable)) {
					root.nextBlock(block);
					for(int event = block.size()-1; event >= 0 ; event--) {
						switch(root.aggregate(block, event)) {
							case INCLUDED:
								return result(entity, queryPlan);
							case NOT_INCLUDED:
								return EntityResult.notContained();
							default:
								//do nothing
						}
					}
				}
				requiredTables.remove(currentTable, Integer.MAX_VALUE);
			}
	
			if(root.isContained()) {
				return result(entity, queryPlan);
			}
			else {
				return EntityResult.notContained();
			}
		} catch(Exception e) {
			return EntityResult.failed(entity.getId(), e);
		}
	}

	private EntityResult result(Entity entity2, QueryPlan queryPlan) {
		String[] values = new String[queryPlan.getAggregators().size()];
		for(int i=0;i<values.length;i++)
			values[i] = Objects.toString(queryPlan.getAggregators().get(i).getAggregationResult());
		return EntityResult.of(entity.getId(), values);
	}

}
