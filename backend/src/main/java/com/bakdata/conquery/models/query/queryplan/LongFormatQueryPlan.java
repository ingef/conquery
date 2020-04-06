package com.bakdata.conquery.models.query.queryplan;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.aggregators.specific.LongFormAggregator.Entry;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LongFormatQueryPlan implements QueryPlan {

	private final QueryPlan subPlan;
	
	@Override
	public QueryPlan clone(CloneContext ctx) {
		return new LongFormatQueryPlan(subPlan.clone(ctx));
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return subPlan.isOfInterest(entity);
	}

	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		EntityResult result = subPlan.execute(ctx, entity);
		
		if(!result.isContained()) {
			return result;
		}
		
		//pivot the last column
		return EntityResult.of(
			result.asContained().getEntityId(),
			result
				.asContained()
				.streamValues()
				.flatMap(arr->Arrays.stream(arr))
				.filter(v->v instanceof List)
				.map(v->(List<Entry>)v)
				.flatMap(List::stream)
				.map(Entry.class::cast)
				.map(e -> new Object[] {e.getColumnId(), e.getValue()})
				.collect(Collectors.toList())
		);
	}
}
