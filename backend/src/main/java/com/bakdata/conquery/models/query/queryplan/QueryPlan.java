package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor(access=AccessLevel.PRIVATE)
public class QueryPlan implements Cloneable {
	private QPNode root;
	private final List<Aggregator<?>> aggregators = new ArrayList<>();

	public static QueryPlan create() {
		QueryPlan plan = new QueryPlan();

		//TODO move to constructor?
		plan.aggregators.add(new SpecialDateUnion());

		return plan;
	}

	public SpecialDateUnion getIncluded() {
		return (SpecialDateUnion) aggregators.get(0);
	}

	@Override
	public QueryPlan clone() {
		QueryPlan clone = new QueryPlan();
		for(Aggregator<?> agg:aggregators)
			clone.aggregators.add(agg.clone());
		clone.root = root.clone(this, clone);
		return clone;
	}
}
