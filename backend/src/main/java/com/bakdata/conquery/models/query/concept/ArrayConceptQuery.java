package com.bakdata.conquery.models.query.concept;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Query type that combines a set of {@link ConceptQuery}s which are separately evaluated
 * and whose results are merged. If a SpecialDateUnion is required, the result will hold
 * the union of all dates from the separate queries.
 */
@Getter
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
@CPSType(id = "ARRAY_CONCEPT_QUERY", base = QueryDescription.class)
public class ArrayConceptQuery extends IQuery {

	@NonNull
	private final List<ConceptQuery> childQueries;

	public static ArrayConceptQuery createFromFeatures(List<CQElement> features) {
		List<ConceptQuery> cqWraps = features.stream()
											 .map(ConceptQuery::new)
											 .collect(Collectors.toList());
		return new ArrayConceptQuery(cqWraps);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		childQueries.forEach(c -> c.resolve(context));
	}

	@Override
	public ArrayConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		// Make sure the constructor and the adding is called with the same context.
		ArrayConceptQueryPlan aq = new ArrayConceptQueryPlan(context);
		aq.addChildPlans(childQueries, context);
		return aq;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecutionId> requiredQueries) {
		childQueries.forEach(q -> q.collectRequiredQueries(requiredQueries));
	}

	@Override
	public void collectResultInfos(ResultInfoCollector collector) {
		List<ResultInfo> infos = collector.getInfos();
		int lastIndex = Math.max(0,infos.size()-1);
		childQueries.forEach(q -> q.collectResultInfos(collector));
		ResultInfo dateInfo = ConqueryConstants.DATES_INFO;
		
		if(!infos.isEmpty()) {
			// Remove DateInfo from each childQuery			
			infos.subList(lastIndex, infos.size()).removeAll(List.of(dateInfo));
		}
		// Add one DateInfo for the whole Query
		collector.getInfos().add(0, dateInfo);
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		childQueries.forEach(q -> q.visit(visitor));
	}
}
