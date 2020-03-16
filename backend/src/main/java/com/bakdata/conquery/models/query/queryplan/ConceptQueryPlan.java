package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.ContainedEntityResult;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@ToString
public class ConceptQueryPlan implements QueryPlan, EventIterating {

	@Getter @Setter
	private Set<Table> requiredTables;
	private QPNode child;
	@ToString.Exclude
	private SpecialDateUnion specialDateUnion = new SpecialDateUnion();
	@ToString.Exclude
	protected final List<Aggregator<?>> aggregators = new ArrayList<>();
	private Entity entity;
	
	public ConceptQueryPlan(boolean generateSpecialDateUnion) {
		if(generateSpecialDateUnion) {
			aggregators.add(specialDateUnion);
		}
	}
	
	public ConceptQueryPlan(QueryPlanContext ctx) {
		this(ctx.isGenerateSpecialDateUnion());
	}

	@Override
	public ConceptQueryPlan clone(CloneContext ctx) {
		checkRequiredTables(ctx.getStorage());
		
		ConceptQueryPlan clone = new ConceptQueryPlan(false);
		clone.setChild(child.clone(ctx));
		for(Aggregator<?> agg:aggregators)
			clone.aggregators.add(agg.clone(ctx));
		clone.specialDateUnion = specialDateUnion.clone(ctx);
		clone.setRequiredTables(this.getRequiredTables());
		return clone;
	}
	
	private void checkRequiredTables(WorkerStorage storage) {
		if(requiredTables == null) {
			synchronized (this) {
				if(requiredTables == null) {
					requiredTables = this.collectRequiredTables()
						.stream()
						.map(storage.getDataset().getTables()::getOrFail)
						.collect(Collectors.toSet());
				}
			}
		}
	}

	public void init(Entity entity) {
		this.entity = entity;
		child.init(entity);
	}

	public void nextEvent(Bucket bucket, int event) {
		getChild().nextEvent(bucket, event);
	}
	
	protected EntityResult result() {
		Object[] values = new Object[aggregators.size()];
		for(int i=0;i<values.length;i++)
			values[i] = aggregators.get(i).getAggregationResult();
		return EntityResult.of(entity.getId(), values);
	}

	public EntityResult createResult() {
		if(isContained()) {
			return result();
		}
		return EntityResult.notContained();
	}
	
	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		checkRequiredTables(ctx.getStorage());
		init(entity);
		if (requiredTables.isEmpty()) {
			return EntityResult.notContained();
		}

		for(Table currentTable : requiredTables) {
			nextTable(ctx, currentTable);
			for(Bucket bucket : entity.getBucket(currentTable.getId())) {
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
		
		return createResult();
	}
	
	@Override
	public void nextTable(QueryExecutionContext ctx, Table currentTable) {
		child.nextTable(ctx, currentTable);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		child.nextBlock(bucket);
	}

	public void addAggregator(Aggregator<?> aggregator) {
		aggregators.add(aggregator);
	}

	public int getAggregatorSize() {
		return aggregators.size();
	}

	public boolean isContained() {
		return child.isContained();
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return child.isOfInterest(entity);
	}

	@Override
	public boolean isOfInterest(Bucket bucket) {
		return child.isOfInterest(bucket);
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		child.collectRequiredTables(requiredTables);
	}
}