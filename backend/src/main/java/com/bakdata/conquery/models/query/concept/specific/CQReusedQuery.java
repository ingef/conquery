package com.bakdata.conquery.models.query.concept.specific;

import java.util.Objects;
import java.util.Set;

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
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.ConceptQuery;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@CPSType(id="SAVED_QUERY", base=CQElement.class)
@RequiredArgsConstructor @AllArgsConstructor(onConstructor_=@JsonCreator)
public class CQReusedQuery implements CQElement {

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
		resolvedQuery = ((ManagedQuery)Objects.requireNonNull(context.getStorage().getExecution(query))).getQuery();
		return this;
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedId> namespacedIds) {
		namespacedIds.add(query);
	}
}
