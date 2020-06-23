package com.bakdata.conquery.models.query.concept.specific;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.concept.NamespacedIdHolding;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id="SAVED_QUERY", base=CQElement.class)
@RequiredArgsConstructor @AllArgsConstructor(onConstructor_=@JsonCreator)
public class CQReusedQuery implements CQElement, NamespacedIdHolding {

	@Getter @NotNull @Valid
	private final ManagedExecutionId query;
	@Getter @InternalOnly
	private IQuery resolvedQuery;

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		requiredQueries.add(query);
	}
	
	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		return ((ConceptQuery)resolvedQuery).getRoot().createQueryPlan(context, plan);
	}
	
	@Override
	public CQElement resolve(QueryResolveContext context) {
		resolvedQuery = ((ManagedQuery)Objects.requireNonNull(context.getNamespaces().getMetaStorage().getExecution(query), "Unable to resolve stored query")).getQuery();
		return this;
	}
	
	@Override
	public void visit(Consumer<Visitable> visitor) {
		CQElement.super.visit(visitor);
		if(resolvedQuery != null) {
			resolvedQuery.visit(visitor);
		}
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> ids) {
		checkNotNull(ids);
		if(query != null) {
			ids.add(query);
		}
	}
}
