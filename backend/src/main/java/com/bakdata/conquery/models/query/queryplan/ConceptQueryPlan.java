package com.bakdata.conquery.models.query.queryplan;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
@NoArgsConstructor
@ToString
public class ConceptQueryPlan extends SimpleQueryPlan {

	private QPNode child;
	@ToString.Exclude
	private SpecialDateUnion specialDateUnion = new SpecialDateUnion();
	@ToString.Exclude
	protected final List<Aggregator<?>> aggregators = Lists.newArrayList(specialDateUnion);
	private Entity entity;

	@Override
	public ConceptQueryPlan clone(CloneContext ctx) {
		ConceptQueryPlan clone = new ConceptQueryPlan();
		clone.setChild(child.clone(ctx));
		clone.aggregators.clear(); //to remove the default specialdateunion
		for(Aggregator<?> agg:aggregators)
			clone.aggregators.add(agg.clone(ctx));
		clone.specialDateUnion = specialDateUnion.clone(ctx);
		clone.setRequiredTables(this.getRequiredTables());
		return clone;
	}
	
	@Override
	public void init(Entity entity) {
		this.entity = entity;
		child.init(entity);
	}

	@Override
	public void nextEvent(Bucket bucket, int event) {
		getChild().nextEvent(bucket, event);
	}
	
	protected EntityResult result() {
		Object[] values = new Object[aggregators.size()];
		for(int i=0;i<values.length;i++)
			values[i] = aggregators.get(i).getAggregationResult();
		return EntityResult.of(entity.getId(), values);
	}

	@Override
	public EntityResult createResult() {
		if(isContained()) {
			return result();
		}
		else {
			return EntityResult.notContained();
		}
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		child.nextTable(ctx, currentTable);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		child.nextBlock(bucket);
	}

	public void addAggregator(Aggregator<?> aggregator) {
		aggregators.add(aggregator);
	}
	
	public void addAggregator(int index, Aggregator<?> aggregator) {
		aggregators.add(index, aggregator);
	}

	public int getAggregatorSize() {
		return aggregators.size();
	}

	@Override
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
