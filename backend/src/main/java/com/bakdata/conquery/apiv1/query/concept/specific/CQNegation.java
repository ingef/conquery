package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.List;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.query.QueryExecutionContext;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.RequiredEntities;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.specific.NegatingNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;

@CPSType(id="NEGATION", base= CQElement.class)
@Setter
@Getter
public class CQNegation extends CQElement {

	@Valid @NotNull @Getter @Setter
	private CQElement child;

	@Getter @Setter
	@JsonView(View.InternalCommunication.class)
	private DateAggregationAction dateAction;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		Preconditions.checkNotNull(dateAction);
		return new NegatingNode(child.createQueryPlan(context, plan), dateAction);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		Preconditions.checkNotNull(context.getDateAggregationMode());

		dateAction = determineDateAction(context);
		child.resolve(context);
	}

	private DateAggregationAction determineDateAction(QueryResolveContext context) {
		return switch (context.getDateAggregationMode()) {
			case MERGE, NONE, INTERSECT -> DateAggregationAction.BLOCK;
			case LOGICAL -> DateAggregationAction.NEGATE;
		};
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return child.getResultInfos();
	}
	
	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		child.visit(visitor);
	}

	@Override
	public RequiredEntities collectRequiredEntities(QueryExecutionContext context) {
		return getChild().collectRequiredEntities(context);
	}
}
