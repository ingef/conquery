package com.bakdata.eva.forms.queries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import com.bakdata.conquery.models.query.results.SinglelineContainedEntityResult;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PSMFormQueryPlan implements QueryPlan {

	private final RelativeFormQueryPlan controlPlan;
	private final RelativeFormQueryPlan featurePlan;
	private final List<TableId> requiredTables;
	private Entity entity;
	
	@Override
	public void init(Entity entity) {
		this.entity = entity;
		controlPlan.init(entity);
		featurePlan.init(entity);
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		requiredTables.addAll(this.requiredTables);
	}
	
	@Override
	public boolean isContained() {
		return controlPlan.isContained() | featurePlan.isContained();
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		controlPlan.nextTable(ctx, currentTable);
		featurePlan.nextTable(ctx, currentTable);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		controlPlan.nextBlock(bucket);
		featurePlan.nextBlock(bucket);
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return
			controlPlan.isOfInterest(bucket)
			|
			featurePlan.isOfInterest(bucket);
	}
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		controlPlan.nextEvent(bucket, event);
		featurePlan.nextEvent(bucket, event);
	}
	
	@Override
	public EntityResult createResult() {
		if(!isContained()) {
			return EntityResult.notContained();
		}
		else {
			List<Object[]> values = new ArrayList<>();
			for(EntityResult res : Arrays.asList(featurePlan.createResult(), controlPlan.createResult())) {
				if(res instanceof SinglelineContainedEntityResult) {
					values.add(((SinglelineContainedEntityResult) res).getValues());
				}
				else if(res instanceof MultilineContainedEntityResult) {
					values.addAll(((MultilineContainedEntityResult) res).getValues());
				}
			}
			return EntityResult.multilineOf(entity.getId(), values);
		}
	}
	
	public int getAggregatorSize() {
		return featurePlan.getAggregatorSize();
	}

	@Override
	public QueryPlan clone(CloneContext ctx) {
		return new PSMFormQueryPlan(
			controlPlan.clone(ctx),
			featurePlan.clone(ctx),
			requiredTables
		);
	}

	@Override
	public void addAggregator(Aggregator<?> aggregator) {
		featurePlan.addAggregator(aggregator);
		controlPlan.addAggregator(aggregator);
	}

	@Override
	public void addAggregator(int index, Aggregator<?> aggregator) {
		featurePlan.addAggregator(index, aggregator);
		controlPlan.addAggregator(index, aggregator);
	}

	@Override
	public SpecialDateUnion getSpecialDateUnion() {
		return featurePlan.getSpecialDateUnion();
	}
}
