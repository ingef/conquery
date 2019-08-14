package com.bakdata.conquery.models.query.queryplan;

import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;

import lombok.Getter;
import lombok.Setter;

public abstract class SimpleQueryPlan implements QueryPlan, EventIterating {

	@Getter @Setter
	private Set<Table> requiredTables;
	
	public abstract void init(Entity entity);
	public abstract void nextEvent(Bucket bucket, int event);
	public abstract boolean isContained();
	public abstract EntityResult createResult();
	
	@Override
	public void prepareClone(QueryContext context) {
		//collect required tables
		requiredTables = this.collectRequiredTables()
			.stream()
			.map(context.getStorage().getDataset().getTables()::getOrFail)
			.collect(Collectors.toSet());
	}
	
	@Override
	public EntityResult execute(QueryContext ctx, Entity entity) {
		init(entity);
		if (requiredTables.isEmpty()) {
			return EntityResult.notContained();
		}

		for(Table currentTable : requiredTables) {
			nextTable(ctx, currentTable);
			for(Bucket bucket : entity.getBucket(currentTable)) {
				int localEntity = bucket.toLocal(entity.getId());
				if(bucket.containsLocalEntity(localEntity)) {
					if(isOfInterest(bucket)) {
						nextBlock(bucket);
						int start = bucket.getFirstEventOfLocal(localEntity);
						int end = bucket.getLastEventOfLocal(localEntity);
						for(int event = start; event < end ; event++) {
							nextEvent(bucket, event);
						}
					}
				}
			}
		}

		//ugly workaround which we should find a fix for
		EntityResult result = createResult();
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
}
