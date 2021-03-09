package com.bakdata.conquery.models.query.concept;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Getter
@Setter
@CPSType(id = "CONCEPT_QUERY", base = QueryDescription.class)
@Slf4j
public class ConceptQuery extends IQuery {

	@Valid
	@NotNull
	protected CQElement root;

	protected ConceptQueryPlan.DateAggregationMode dateAggregationMode;


	@InternalOnly
	protected ConceptQueryPlan.DateAggregationMode resolvedDateAggregationMode;

	public ConceptQuery(CQElement root, ConceptQueryPlan.DateAggregationMode dateAggregationMode) {
		this.root = root;
		this.dateAggregationMode = dateAggregationMode;
	}

	public ConceptQuery(CQElement root) {
		this(root, null);
	}

	@Override
	public ConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		ConceptQueryPlan qp = new ConceptQueryPlan(resolvedDateAggregationMode);
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
		resolvedDateAggregationMode = dateAggregationMode;
		if(context.getDateAggregationMode() != null) {
			log.trace("Overriding date aggregation mode ({}) with mode from context ({})", dateAggregationMode, context.getDateAggregationMode());
			resolvedDateAggregationMode = context.getDateAggregationMode();
		}

		if (resolvedDateAggregationMode == null) {
			log.trace("No date aggregation mode was availiable. Falling back to MERGE");
			resolvedDateAggregationMode = ConceptQueryPlan.DateAggregationMode.MERGE;

		}

		root.resolve(context.withDateAggregationMode(resolvedDateAggregationMode));
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		Preconditions.checkNotNull(resolvedDateAggregationMode);
		if(!Objects.equals(resolvedDateAggregationMode, ConceptQueryPlan.DateAggregationMode.NONE)) {
			collector.add(ConqueryConstants.DATES_INFO);
		}
		root.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		root.visit(visitor);
	}
}