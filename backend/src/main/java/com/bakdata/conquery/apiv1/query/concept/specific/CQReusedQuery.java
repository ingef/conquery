package com.bakdata.conquery.apiv1.query.concept.specific;

import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.validation.Valid;

import com.bakdata.conquery.apiv1.query.CQElement;
import com.bakdata.conquery.apiv1.query.Query;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@CPSType(id = "SAVED_QUERY", base = CQElement.class)
@NoArgsConstructor(onConstructor_ = @JsonCreator)
@Getter @Setter
public class CQReusedQuery extends CQElement {

	public CQReusedQuery(ManagedExecutionId executionId){
		this.queryId = executionId;
	}

	/**
	 * @implNote Cannot use {@link com.bakdata.conquery.io.jackson.serializer.MetaIdRef} as that would be a dependency on the same store which is not possible, due to eager loading and unordered loading.
	 */
	@Nullable
	@Valid
	@JsonProperty("query")
	private ManagedExecutionId queryId;

	@JsonIgnore
	private ManagedQuery query;

	@InternalOnly
	private Query resolvedQuery;

	private boolean excludeFromSecondaryId = false;

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
		if(query != null) {
			requiredQueries.add(query);
			query.getQuery().collectRequiredQueries(requiredQueries);
		}
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
		query = ((ManagedQuery) context.getDatasetRegistry().getMetaRegistry().resolve(queryId));
		resolvedQuery = query.getQuery();

		// Yey recursion, because the query might consist of another CQReusedQuery or CQExternal
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

}
