package com.bakdata.conquery.models.query.concept.specific;

import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.NegatingNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import lombok.Getter;
import lombok.Setter;

@CPSType(id="NEGATION", base=CQElement.class)
@Setter
@Getter
public class CQNegation extends CQElement {

	@Valid @NotNull @Getter @Setter
	private CQElement child;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		ConceptQueryPlan.DateAggregationAction dateAction = ConceptQueryPlan.DateAggregationAction.MERGE;
		switch(context.getDateAggregationMode()) {
			case MERGE:
			case NONE:
			case INTERSECT:
				dateAction = null;
			case LOGICAL:
				dateAction = ConceptQueryPlan.DateAggregationAction.NEGATE;
				break;
		}

		return new NegatingNode(child.createQueryPlan(context, plan), dateAction);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		child.resolve(context);
	}
	
	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		child.collectResultInfos(collector);
	}
	
	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		child.visit(visitor);
	}
}
