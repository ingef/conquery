package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FormQueryPlan implements QueryPlan {

	private final List<DateContext> dateContexts;
	private final ArrayConceptQueryPlan features;

	@Override
	public EntityResult execute(QueryExecutionContext ctx, Entity entity) {
		List<Object[]> resultValues = new ArrayList<>(dateContexts.size());
		
		for(DateContext dateContext : dateContexts) {
			
			CloneContext clCtx = new CloneContext(ctx.getStorage());
						
			ArrayConceptQueryPlan subPlan = features.clone(clCtx);
	
			CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(dateContext.getDateRange());
			EntityResult subResult = subPlan.execute(ctx.withDateRestriction(dateRestriction), entity);
			
			resultValues.addAll(
				ResultModifier.modify(
					subResult,
					subPlan,
					v->addConstants(v, dateContext)
				)
			);
		}
		
		return EntityResult.multilineOf(entity.getId(), resultValues);
	}
	
	private Object[] addConstants(Object[] values, DateContext dateContext) {
		int constants = dateContext.getEventDate() == null ? 3 : 4;
		
		Object[] result = new Object[values.length+constants];
		System.arraycopy(values, 0, result, constants, values.length);
		
		//add resolution indicator
		result[0] = dateContext.getSubdivisionMode();	
		//add index value
		result[1] = dateContext.getIndex();
		// add event date
		if(dateContext.getEventDate() != null) {
			result[2] = dateContext.getEventDate();
		}
		//add date range at [2] or [3]
		result[constants-1] = dateContext.getDateRange().toString();
		
		return result;
	}

	@Override
	public FormQueryPlan clone(CloneContext ctx) {
		return new FormQueryPlan(dateContexts, features);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return true;
	}
}
