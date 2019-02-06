package com.bakdata.conquery.models.query;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

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
			QPNode root = queryPlan.getRoot();
			root.init(entity);
			
			if (requiredTables.isEmpty()) {
				return EntityResult.notContained();
			}

			for(Table currentTable : requiredTables) {
				root.nextTable(ctx, currentTable);
				for(Block block : entity.getBlocks().get(currentTable)) {
					root.nextBlock(block);
					for(int event = block.size()-1; event >= 0 ; event--) {
						if(!root.aggregate(block, event)) {
							return EntityResult.notContained();
						}
					}
				}
			}
	
			if(root.isContained()) {
				return result(queryPlan);
			}
			else {
				return EntityResult.notContained();
			}
		}
		catch(Exception e) {
			return EntityResult.failed(entity.getId(), e);
		}
	}

	private EntityResult result(QueryPlan queryPlan) {
		String[] values = queryPlan.getAggregators().values().stream()
								   .map(Aggregator::getAggregationResult)
								   .map(val -> val == null ? "" : Objects.toString(val))
								   .toArray(String[]::new);

		return EntityResult.of(entity.getId(), values);
	}

}
