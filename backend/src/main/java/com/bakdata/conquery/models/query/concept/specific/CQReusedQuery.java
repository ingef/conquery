package com.bakdata.conquery.models.query.concept.specific;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.validation.Valid;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.MetaIdRef;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.NamespacedIdentifiable;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.concept.NamespacedIdentifiableHolding;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id = "SAVED_QUERY", base = CQElement.class)
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@Getter @Setter
public class CQReusedQuery extends CQElement implements NamespacedIdentifiableHolding {

	public CQReusedQuery(ManagedQuery query){
		this.query = query;
	}

	@Nullable // Null on Shards, therefore resolved on Manager.
	@Valid
	@MetaIdRef
	private ManagedQuery query;

	@InternalOnly
	private IQuery resolvedQuery;

	private boolean excludeFromSecondaryId = false;

	@Override
	public void collectRequiredQueries(Set<ManagedExecution> requiredQueries) {
		requiredQueries.add(query);
	}

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, ConceptQueryPlan plan) {
		// We shadow the SecondaryId if it is excluded
		if (excludeFromSecondaryId) {
			context = context.withSelectedSecondaryId(null);
		}

		return resolvedQuery.getReusableComponents()
							.createQueryPlan(context, plan);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		resolvedQuery = query.getQuery();

		// Yey recursion, because the query might consists of another CQReusedQuery or CQExternal
		resolvedQuery.resolve(context);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		super.visit(visitor);
		if (resolvedQuery != null) {
			resolvedQuery.visit(visitor);
		}
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		resolvedQuery.getReusableComponents().collectResultInfos(collector);
	}

	@Override
	public void collectNamespacedIds(Set<NamespacedIdentifiable<?>> ids) {
		checkNotNull(ids);
		ids.add(query);
	}
}
