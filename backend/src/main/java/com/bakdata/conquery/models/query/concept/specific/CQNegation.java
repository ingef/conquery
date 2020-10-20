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
		return new NegatingNode(child.createQueryPlan(context.withGenerateSpecialDateUnion(false), plan));
	}

	@Override
	public CQElement resolve(QueryResolveContext context) {
		child = child.resolve(context);
		return this;
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
