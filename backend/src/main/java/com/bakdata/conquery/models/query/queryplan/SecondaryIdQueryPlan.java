package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.SinglelineEntityResult;
import com.bakdata.conquery.models.types.specific.AStringType;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter @Setter
public class SecondaryIdQueryPlan implements QueryPlan {

	private final ConceptQueryPlan query;
	private final String secondaryId;
	private Column currentSecondaryIdColumn;
	private Map<String, ConceptQueryPlan> childPerKey = new HashMap<>();
	
	private Column findSecondaryIdColumn(Table table) {
		for(var col:table.getColumns()) {
			if(secondaryId.equals(col.getSecondaryId())) {
				return col;
			}
		}
		
		throw new IllegalStateException("Table "+table+" should not appear in a query about secondary id "+secondaryId);
	}

	private ConceptQueryPlan createChild(Object key, QueryExecutionContext currentContext, Bucket currentBucket) {
		ConceptQueryPlan plan = query.clone(new CloneContext(currentContext.getStorage()));
		plan.init(query.getEntity());
		plan.nextTable(currentContext, currentSecondaryIdColumn.getTable());
		plan.isOfInterest(currentBucket);
		plan.nextBlock(currentBucket);
		return plan;
	}
	
	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		query.checkRequiredTables(ctx.getStorage());
		query.init(entity);
		if (query.getRequiredTables().isEmpty()) {
			return EntityResult.notContained();
		}

		for(Table currentTable : query.getRequiredTables()) {
			currentSecondaryIdColumn = findSecondaryIdColumn(currentTable);
			query.nextTable(ctx, currentTable);
			for(Bucket bucket : entity.getBucket(currentTable.getId())) {
				int localEntity = bucket.toLocal(entity.getId());
				var secondaryIdType = (AStringType<?>)currentSecondaryIdColumn.getTypeFor(bucket);
				query.nextBlock(bucket);
				if(bucket.containsLocalEntity(localEntity)) {
					if(query.isOfInterest(bucket)) {
						int start = bucket.getFirstEventOfLocal(localEntity);
						int end = bucket.getLastEventOfLocal(localEntity);
						for(int event = start; event < end ; event++) {
							String key = secondaryIdType.getElement(bucket.getString(event, currentSecondaryIdColumn));
							childPerKey
								.computeIfAbsent(key, k->this.createChild(k, ctx, bucket))
								.nextEvent(bucket, event);
						}
					}
				}
			}
		}
		
		
		var result = new ArrayList<Object[]>(childPerKey.values().size());
		for(var child:childPerKey.entrySet()) {
			if(child.getValue().isContained()) {
				result.add(ArrayUtils.insert(0, child.getValue().result().getValues(), child.getKey()));
			}
		}
		if(result.isEmpty()) {
			return EntityResult.notContained();
		}
		return EntityResult.multilineOf(entity.getId(), result);
	}

	@Override
	public QueryPlan clone(CloneContext ctx) {
		return new SecondaryIdQueryPlan(query.clone(ctx), secondaryId);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return query.isOfInterest(entity);
	}
}
