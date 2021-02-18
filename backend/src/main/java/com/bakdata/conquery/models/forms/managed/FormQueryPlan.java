package com.bakdata.conquery.models.forms.managed;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.forms.util.DateContext;
import com.bakdata.conquery.models.forms.util.ResultModifier;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.entity.Entity;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.aggregators.Aggregator;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.bakdata.conquery.models.query.results.MultilineContainedEntityResult;
import lombok.Getter;

@Getter
public class FormQueryPlan implements QueryPlan {

	private final List<DateContext> dateContexts;
	private final ArrayConceptQueryPlan features;
	private final int constantCount;
	
	public FormQueryPlan(List<DateContext> dateContexts, ArrayConceptQueryPlan features) {
		this.dateContexts = dateContexts;
		this.features = features;
		
		if (dateContexts.size() <= 0) {
			// There is nothing to do for this FormQueryPlan but we will return an empty result when its executed
			constantCount = 3;
			return;
		}
		
		// Either all date contexts have an relative event date or none has one
		boolean withRelativeEventdate = dateContexts.get(0).getEventDate() != null;
		for(DateContext dateContext : dateContexts) {
			if((dateContext.getEventDate() != null) != withRelativeEventdate) {
				throw new IllegalStateException("Queryplan has absolute AND relative date contexts. Only one kind is allowed.");
			}
		}
		constantCount = withRelativeEventdate ? 4 : 3; // resolution indicator, index value, (event date,) date range
	}

	@Override
	public MultilineContainedEntityResult execute(QueryExecutionContext ctx, Entity entity) {

		features.init(ctx,entity);

		if (!isOfInterest(entity)) {
			return createResultForNotContained(entity, null);
		}

		List<Object[]> resultValues = new ArrayList<>(dateContexts.size());
		
		for(DateContext dateContext : dateContexts) {
			
			CloneContext clCtx = new CloneContext(ctx.getStorage());
						
			ArrayConceptQueryPlan subPlan = features.clone(clCtx);
	
			CDateSet dateRestriction = CDateSet.create(ctx.getDateRestriction());
			dateRestriction.retainAll(dateContext.getDateRange());
			EntityResult subResult = subPlan.execute(ctx.withDateRestriction(dateRestriction), entity);
			
			if(subResult.isFailed()) {
				throw subResult.asFailed().getError();
			}
			
			if(!subResult.isContained()) {
				resultValues.addAll(createResultForNotContained(entity, dateContext).listResultLines());
				continue;
			}
			
			resultValues.addAll(
				ResultModifier.modify(
					subResult.asContained(),
					ResultModifier.existAggValuesSetterFor(subPlan.getAggregators(), OptionalInt.of(0)).unaryAndThen(v->addConstants(v, dateContext))
				).listResultLines()
			);
		}
		
		return EntityResult.multilineOf(entity.getId(), resultValues);
	}

	private MultilineContainedEntityResult createResultForNotContained(Entity entity, DateContext dateContext) {
		List<Object[]> result = new ArrayList<>();
		result.add(new Object[getAggregators().size()]);
		return ResultModifier.modify(EntityResult.multilineOf(entity.getId(), result), ResultModifier.existAggValuesSetterFor(getAggregators(), OptionalInt.of(0)).unaryAndThen( v->addConstants(v, dateContext)));
	}
	
	public List<Aggregator<?>> getAggregators() {
		return features.getAggregators();
	}
	
	private Object[] addConstants(Object[] values, DateContext dateContext) {		
		Object[] result = new Object[values.length + constantCount];
		System.arraycopy(values, 0, result, constantCount, values.length);
		
		if(dateContext == null) {
			return result;
		}
		
		//add resolution indicator
		result[0] = dateContext.getSubdivisionMode().toString();
		//add index value
		result[1] = dateContext.getIndex();
		// add event date
		if(dateContext.getEventDate() != null) {
			result[2] = dateContext.getEventDate().toEpochDay();
		}
		//add date range at [2] or [3]
		result[constantCount-1] = dateContext.getDateRange();
		
		return result;
	}

	@Override
	public FormQueryPlan clone(CloneContext ctx) {
		return new FormQueryPlan(dateContexts, features);
	}

	@Override
	public boolean isOfInterest(Entity entity) {
		return features.isOfInterest(entity);
	}
	
	public int columnCount() {
		return constantCount + features.getAggregatorSize();
	}
}
