package com.bakdata.conquery.models.query.concept.specific;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.NegatingNode;

@CPSType(id="NEGATION", base=CQElement.class)
public class CQNegation implements CQElement {

	@Valid @NotNull
	private CQElement child;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		return new NegatingNode(child.createQueryPlan(context, plan));
	}
}
