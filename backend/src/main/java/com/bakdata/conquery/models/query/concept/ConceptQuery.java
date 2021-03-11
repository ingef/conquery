package com.bakdata.conquery.models.query.concept;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.*;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@JsonCreator))
public class ConceptQuery extends IQuery {

	@Valid
	@NotNull
	protected CQElement root;

	@NotNull
	protected DateAggregationMode dateAggregationMode = DateAggregationMode.MERGE;


	@InternalOnly
	protected DateAggregationMode resolvedDateAggregationMode;

	public ConceptQuery(CQElement root, DateAggregationMode dateAggregationMode) {
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
		root.resolve(context.withDateAggregationMode(resolvedDateAggregationMode));
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		Preconditions.checkNotNull(resolvedDateAggregationMode);
		if(!Objects.equals(resolvedDateAggregationMode, DateAggregationMode.NONE)) {
			collector.add(ConqueryConstants.DATES_INFO);
		}
		root.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		root.visit(visitor);
	}

	@Override
	public CQElement getReusableComponents() {
		return getRoot();
	}
}