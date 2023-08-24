package com.bakdata.conquery.apiv1.query;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.bakdata.conquery.apiv1.query.concept.specific.temporal.CQTemporal;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.query.queryplan.TimeBasedQueryPlan;
import com.bakdata.conquery.models.query.queryplan.specific.temporal.TemporalSubQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.results.EntityResult;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@CPSType(id = "TIME_QUERY", base = QueryDescription.class)
//TODO this should happen in query planning
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
public class TimeBasedQuery extends Query {

	private final Query query;



	@Override
	public QueryPlan<?> createQueryPlan(QueryPlanContext context) {
		final TimeBasedQueryPlan timeBasedQueryPlan = new TimeBasedQueryPlan();

		final List<CQTemporal> temporalQueries =
				Visitable.stream(query)
						 .filter(CQTemporal.class::isInstance)
						 .map(CQTemporal.class::cast)
						 .toList();

		timeBasedQueryPlan.setSubQuery((QueryPlan<EntityResult>) query.createQueryPlan(context));

		// CQTemporal#createQueryPlan maintains state for us that we collect here!
		final List<TemporalSubQueryPlan> temporalSubQueryPlans =
				temporalQueries.stream().map(TimeBasedQuery::collectSubQuery).toList();

		timeBasedQueryPlan.setTemporalSubPlans(temporalSubQueryPlans);


		return timeBasedQueryPlan;
	}

	private static TemporalSubQueryPlan collectSubQuery(CQTemporal tq) {
		return new TemporalSubQueryPlan(tq.getSelector(),tq.getMode(), tq.getIndexPlan(), tq.getPreceedingPlan(), tq);
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		query.collectRequiredQueries(requiredQueries);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		query.resolve(context);
	}

	@Override
	public List<ResultInfo> getResultInfos() {
		return query.getResultInfos();
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		query.visit(visitor);
	}
}
