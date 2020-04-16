package com.bakdata.conquery.models.query.concept;

import java.util.Set;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.externalservice.ResultType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.LongFormatQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.bakdata.conquery.models.query.resultinfo.SimpleResultInfo;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@CPSType(id = "LONG_FORMAT_QUERY", base = QueryDescription.class)
@RequiredArgsConstructor(onConstructor = @__({@JsonCreator}))
public class LongFormatQuery extends IQuery {

	@Valid
	@NotNull @NonNull
	protected IQuery query;

	@Override
	public LongFormatQueryPlan createQueryPlan(QueryPlanContext context) {
		return new LongFormatQueryPlan(
			query.createQueryPlan(
				context.withResultFormat(ConceptQueryResultFormat.LONG)
			)
		);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public LongFormatQuery resolve(QueryResolveContext context) {
		this.query = query.resolve(context);
		return this;
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		collector.add(new SimpleResultInfo("column", ResultType.CATEGORICAL));
		collector.add(new SimpleResultInfo("value", ResultType.STRING));
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		query.visit(visitor);
	}
}