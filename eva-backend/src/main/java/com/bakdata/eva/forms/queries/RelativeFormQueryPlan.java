package com.bakdata.eva.forms.queries;

import java.util.ArrayList;
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

import lombok.Getter;

@Getter
public class RelativeFormQueryPlan implements QueryPlan {
	private final FormQueryPlan featurePlan;
	private final FormQueryPlan outcomePlan;
	private Entity entity;
	
	public RelativeFormQueryPlan(FormQueryPlan featurePlan, FormQueryPlan outcomePlan) {
		this.featurePlan = featurePlan;
		this.outcomePlan = outcomePlan;
	}
	
	@Override
	public void init(Entity entity) {
		this.entity = entity;
		featurePlan.init(entity);
		outcomePlan.init(entity);
	}
	
	@Override
	public void collectRequiredTables(Set<TableId> requiredTables) {
		featurePlan.collectRequiredTables(requiredTables);
		outcomePlan.collectRequiredTables(requiredTables);
	}
	
	@Override
	public boolean isContained() {
		return featurePlan.isContained();
	}
	
	@Override
	public void nextTable(QueryContext ctx, Table currentTable) {
		featurePlan.nextTable(ctx, currentTable);
		outcomePlan.nextTable(ctx, currentTable);
	}
	
	@Override
	public void nextBlock(Bucket bucket) {
		featurePlan.nextBlock(bucket);
		outcomePlan.nextBlock(bucket);
	}
	
	@Override
	public boolean isOfInterest(Bucket bucket) {
		return
			featurePlan.isOfInterest(bucket)
			|
			featurePlan.isOfInterest(bucket);
	}
	
	@Override
	public void nextEvent(Bucket bucket, int event) {
		featurePlan.nextEvent(bucket, event);
		outcomePlan.nextEvent(bucket, event);
	}
	
	@Override
	public EntityResult createResult() {
		if(!isContained()) {
			return EntityResult.notContained();
		}
		else {
			List<Object[]> values = new ArrayList<>();
			MultilineContainedEntityResult featureResult = (MultilineContainedEntityResult)featurePlan.createResult();
			MultilineContainedEntityResult outcomeResult = (MultilineContainedEntityResult)outcomePlan.createResult();
			int featureLength = featurePlan.getAggregatorSize();
			int size = featurePlan.getAggregatorSize() + outcomePlan.getAggregatorSize() - (2+featurePlan.getAggregatorOffset());
			
			//merge the full (index == null) lines
			Object[] mergedFull = new Object[size];
			setFeatureValues(mergedFull, featureResult.getValues().get(0));
			setOutcomeValues(mergedFull, outcomeResult.getValues().get(0), featureLength);
			values.add(mergedFull);
			
			//append all other lines directly
			for(int i=1; i<featureResult.getValues().size(); i++) {
				Object[] result = new Object[size];
				setFeatureValues(result, featureResult.getValues().get(i));
				values.add(result);
			}
			for(int i=1; i<outcomeResult.getValues().size(); i++) {
				Object[] result = new Object[size];
				setOutcomeValues(result, outcomeResult.getValues().get(i), featureLength);
				values.add(result);
			}
			return EntityResult.multilineOf(entity.getId(), values);
		}
	}
	
	private void setOutcomeValues(Object[] result, Object[] value, int featureLength) {
		int off = featurePlan.getAggregatorOffset();
		//copy everything up to including index
		for(int i=0;i<2+off;i++) {
			result[i] = value[i];
		}
		//copy daterange
		result[3+off] = value[2+off];
		System.arraycopy(value, 3+off, result, 1 + featureLength, value.length - (3+off));
	}

	private void setFeatureValues(Object[] result, Object[] value) {
		int off = featurePlan.getAggregatorOffset();
		//copy everything up to including index
		for(int i=0;i<2+off;i++) {
			result[i] = value[i];
		}
		//copy daterange
		result[2+off] = value[2+off];
		System.arraycopy(value, 3+off, result, 4+off, value.length - (3+off));
	}

	@Override
	public RelativeFormQueryPlan clone(CloneContext ctx) {
		return new RelativeFormQueryPlan(featurePlan.clone(ctx), outcomePlan.clone(ctx));
	}

	@Override
	public void addAggregator(Aggregator<?> aggregator) {
		featurePlan.addAggregator(aggregator);
		outcomePlan.addAggregator(aggregator);
	}

	@Override
	public void addAggregator(int index, Aggregator<?> aggregator) {
		featurePlan.addAggregator(index, aggregator);
		outcomePlan.addAggregator(index, aggregator);
	}

	@Override
	public SpecialDateUnion getSpecialDateUnion() {
		return featurePlan.getSpecialDateUnion();
	}

	@Override
	public int getAggregatorSize() {
		return featurePlan.getAggregatorSize();
	}
}
