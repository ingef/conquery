package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor(access=AccessLevel.PUBLIC)
public class ConceptQueryPlan extends QPChainNode implements Cloneable, QueryPlan {
	
	protected final List<Aggregator<?>> aggregators = new ArrayList<>();
	private SpecialDateUnion specialDateUnion = new SpecialDateUnion();

	public static ConceptQueryPlan create() {
		ConceptQueryPlan plan = new ConceptQueryPlan();
		plan.aggregators.add(plan.specialDateUnion);

		return plan;
	}
	
	@Override
	public ConceptQueryPlan createClone() {
		return (ConceptQueryPlan)QueryPlan.super.createClone();
	}
	
	@Override
	public ConceptQueryPlan clone(CloneContext ctx) {
		return ctx.clone(this);
	}

	@Override
	public ConceptQueryPlan doClone(CloneContext ctx) {
		ConceptQueryPlan clone = new ConceptQueryPlan();
		for(Aggregator<?> agg:aggregators)
			clone.aggregators.add(agg.clone(ctx));
		clone.specialDateUnion = specialDateUnion.clone(ctx);
		clone.setChild(getChild().clone(ctx));
		return clone;
	}
	
	@Override
	public void nextEvent(Block block, int event) {
		getChild().nextEvent(block, event);
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
	public void addAggregator(Aggregator<?> aggregator) {
		aggregators.add(aggregator);
	}
	
	@Override
	public void addAggregator(int index, Aggregator<?> aggregator) {
		aggregators.add(index, aggregator);
	}

	@Override
	public int getAggregatorSize() {
		return aggregators.size();
	}
}
