package com.bakdata.conquery.models.query.concept;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "CONCEPT_QUERY", base = QueryDescription.class)
public class ConceptQuery extends IQuery {

	@Valid
	@NotNull
	protected CQElement root;

	@NotNull
	protected ConceptQueryPlan.DateAggregationMode dateAggregationMode = ConceptQueryPlan.DateAggregationMode.MERGE;

	private ConceptQuery () {

	}

	public ConceptQuery(CQElement root) {
		this.root = root;
	}

	public ConceptQuery(CQElement root, ConceptQueryPlan.DateAggregationMode dateAggregationMode) {
		this(root);
		this.dateAggregationMode = dateAggregationMode;
	}

	@Override
	public ConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		if(context.getDateAggregationMode() == null) {
			// Set aggregation mode if none was defined.
			context = context.withDateAggregationMode(dateAggregationMode);
		}
		ConceptQueryPlan qp = new ConceptQueryPlan(context.withDateAggregationMode(dateAggregationMode));
		qp.setChild(root.createQueryPlan(context, qp));
		qp.getDateAggregator().register(qp.getChild().getDateAggregators());
		return qp;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		root.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		root.resolve(context);
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add(ConqueryConstants.DATES_INFO);
		root.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		root.visit(visitor);
	}
}