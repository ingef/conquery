package com.bakdata.conquery.models.query.concept;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.SecondaryIdQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "SECONDARY_ID_QUERY", base = IQuery.class)
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class SecondaryIdQuery implements IQuery {

	@Valid
	@NotNull @NonNull
	protected ConceptQuery query;
	@NotNull @NonNull
	protected SecondaryId secondaryId;

	@Override
	public SecondaryIdQueryPlan createQueryPlan(QueryPlanContext context) {
		return new SecondaryIdQueryPlan(query.createQueryPlan(context), secondaryId);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public SecondaryIdQuery resolve(QueryResolveContext context) {
		this.query = query.resolve(context);
		return this;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add( new SimpleResultInfo(secondaryId.getName(), ResultType.STRING));
		query.collectResultInfos(collector);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}