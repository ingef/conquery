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

@Getter @Setter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class QueryPlan extends QPChainNode implements Cloneable {
	private final List<Aggregator<?>> aggregators = new ArrayList<>();

	public static QueryPlan create() {
		QueryPlan plan = new QueryPlan();

		plan.aggregators.add(new SpecialDateUnion());

		return plan;
	}

	/**
	 * Returns the special Aggregator representing the time the events have been included.
	 * @return this {@link QueryPlan}'s SpecialDateUnion
	 */
	public SpecialDateUnion getIncluded() {
		return (SpecialDateUnion) aggregators.get(0);
	}

	@Override
	public QueryPlan clone() {
		return clone(this, new QueryPlan());
	}

	public Stream<QueryPart> execute(QueryContext context, Collection<Entity> entries) {
		//collect required tables
		Set<Table> requiredTables = this
			.getChild()
			.collectRequiredTables()
			.stream()
			.map(context.getStorage().getDataset().getTables()::getOrFail)
			.collect(Collectors.toSet());
		
		return entries
			.stream()
			.map(entity -> new QueryPart(context, this, requiredTables, entity));
	}

	@Override
	public QueryPlan clone(QueryPlan plan, QueryPlan clone) {
		for(Aggregator<?> agg:aggregators)
			clone.aggregators.add(agg.clone());
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
			values[i] = Objects.toString(aggregators.get(i).getAggregationResult());
		return EntityResult.of(entity.getId(), values);
	}

	public EntityResult createResult() {
		if(isContained()) {
			return result();
		}
		else {
			return EntityResult.notContained();
		}
	}
}
