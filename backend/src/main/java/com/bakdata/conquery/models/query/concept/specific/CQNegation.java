package com.bakdata.conquery.models.query.concept.specific;

import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.SubQueryNode;
import com.bakdata.conquery.models.query.queryplan.specific.NegatingNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;

@CPSType(id="NEGATION", base=CQElement.class)
@Setter
@Getter
public class CQNegation extends CQElement {

	@Valid
	private CQElement child;

	@InternalOnly
	private DateAggregationAction dateAction;

	/**
	 * If true the child is evaluated as a sub query.
	 */
	private boolean asSubquery = true;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		Preconditions.checkNotNull(dateAction);
		if (asSubquery) {
			ConceptQueryPlan qp = new ConceptQueryPlan(false);
			qp.setChild(new NegatingNode(child.createQueryPlan(context, qp), dateAction));
			qp.getDateAggregator().registerAll(qp.getChild().getDateAggregators());
			plan.addSubquery(qp);
			return new SubQueryNode(qp);
		}

		return new NegatingNode(child.createQueryPlan(context, plan), dateAction);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		Preconditions.checkNotNull(context.getDateAggregationMode());

		dateAction = determineDateAction(context);
		child.resolve(context);
	}

	private DateAggregationAction determineDateAction(QueryResolveContext context) {
		switch(context.getDateAggregationMode()) {
			case MERGE:
			case NONE:
			case INTERSECT:
				return DateAggregationAction.BLOCK;
			case LOGICAL:
				return DateAggregationAction.NEGATE;
			default:
				throw new IllegalStateException("Cannot handle mode " + context.getDateAggregationMode());
		}
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
