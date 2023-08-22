package com.bakdata.conquery.apiv1.query;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQAbstractTemporalQuery;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ConceptQueryPlan;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.TimeBasedQueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import lombok.Data;

@Data
public class TimeBasedQuery extends Query {

	private final Query query;

	@Override
	public QueryPlan<?> createQueryPlan(QueryPlanContext context) {
		final TimeBasedQueryPlan timeBasedQueryPlan = new TimeBasedQueryPlan();

		final List<CQAbstractTemporalQuery> temporalQueries = Visitable.stream(query)
																	   .filter(CQAbstractTemporalQuery.class::isInstance)
																	   .map(CQAbstractTemporalQuery.class::cast)
																	   .toList();

		final List<TemporalSubQueryPlan> temporalSubQueryPlans = temporalQueries.stream()
																				.map(tq -> createSubQuery(context, tq)).toList();

		timeBasedQueryPlan.setTemporalSubPlans(temporalSubQueryPlans);

		timeBasedQueryPlan.setSubQuery((QueryPlan<EntityResult>) query.createQueryPlan(context));


		return timeBasedQueryPlan;
	}

	private static TemporalSubQueryPlan createSubQuery(QueryPlanContext context, CQAbstractTemporalQuery tq) {
		tq.createQueryPlan(context, new ConceptQueryPlan(true)); //TODO caputre CQP?

		return new TemporalSubQueryPlan(TemporalSubQueryPlan.Mode.ANY, tq.getIndexPlan(), tq.getPreceedingPlan(), tq);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {

	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return null;
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {

	}
}
