package com.bakdata.conquery.models.query.queryplan;

import com.bakdata.conquery.models.identifiable.ids.specific.SelectId;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.SpecialDateUnion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryPlan implements Cloneable {
	private QPNode root;

	private final Map<SelectId, Aggregator<?>> aggregators = new HashMap<>();


	public static QueryPlan create() {
		QueryPlan plan = new QueryPlan();
		plan.aggregators.put(null, new SpecialDateUnion(null));
		return plan;
	}

	@Override
	public QueryPlan clone() {
		QueryPlan clone = new QueryPlan();

		for (Map.Entry<SelectId, Aggregator<?>> agg : aggregators.entrySet())
			clone.aggregators.put(agg.getKey(), agg.getValue().clone());

		clone.root = root.clone(this, clone);
		return clone;
	}
}
