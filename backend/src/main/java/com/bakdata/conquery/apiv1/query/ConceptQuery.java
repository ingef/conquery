package com.bakdata.conquery.apiv1.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@CPSType(id = "CONCEPT_QUERY", base = QueryDescription.class)
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@JsonCreator))
public class ConceptQuery extends Query {

	@Valid
	@NotNull
	protected CQElement root;

	@NotNull
	protected DateAggregationMode dateAggregationMode = DateAggregationMode.MERGE;


	@JsonView(View.InternalCommunication.class)
	protected DateAggregationMode resolvedDateAggregationMode;

	public ConceptQuery(CQElement root, DateAggregationMode dateAggregationMode) {
		this(root);
		this.dateAggregationMode = dateAggregationMode;
	}

	public ConceptQuery(CQElement root) {
		this.root = root;
	}

	@Override
	public ConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		ConceptQueryPlan qp = new ConceptQueryPlan(!DateAggregationMode.NONE.equals(resolvedDateAggregationMode));
		qp.setChild(root.createQueryPlan(context, qp));
		qp.getDateAggregator().registerAll(qp.getChild().getDateAggregators());
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
	public List<ResultInfo> getResultInfos() {
		Preconditions.checkNotNull(resolvedDateAggregationMode);
		List<ResultInfo> resultInfos = new ArrayList<>();
		if(!DateAggregationMode.NONE.equals(resolvedDateAggregationMode)) {
			resultInfos.add(ConqueryConstants.DATES_INFO);
		}
		resultInfos.addAll(root.getResultInfos());

		return resultInfos;
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