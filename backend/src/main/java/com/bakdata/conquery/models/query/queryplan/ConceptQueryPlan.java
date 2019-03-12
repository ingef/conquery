package com.bakdata.conquery.models.query.queryplan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.query.QueryContext;
import com.bakdata.conquery.models.query.QueryPart;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
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
	public ConceptQueryPlan clone() {
		return clone(this, new ConceptQueryPlan());
	}

	@Override
	public ConceptQueryPlan clone(QueryPlan plan, QueryPlan clone) {
		return clone((ConceptQueryPlan)plan, (ConceptQueryPlan)clone);
	}
	
	public ConceptQueryPlan clone(ConceptQueryPlan plan, ConceptQueryPlan clone) {
		for(Aggregator<?> agg:aggregators)
			clone.aggregators.add(agg.clone());
		clone.specialDateUnion = (SpecialDateUnion) clone.aggregators.get(aggregators.indexOf(specialDateUnion));
		clone.setChild(getChild().clone(this, clone));
		return clone;
	}

	@Override
	public boolean nextEvent(Block block, int event) {
		return getChild().nextEvent(block, event);
	}
	
	protected EntityResult result() {
		String[] values = new String[aggregators.size()];
		for(int i=0;i<values.length;i++)
			values[i] = Objects.toString(aggregators.get(i).getAggregationResult(), "");
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
	public <T> Aggregator<T> getCloneOf(QueryPlan originalPlan, Aggregator<T> aggregator) {
		return (Aggregator<T>) aggregators.get(((ConceptQueryPlan)originalPlan).aggregators.indexOf(aggregator));
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
