package com.bakdata.conquery.apiv1.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.query.DateAggregationMode;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.models.query.queryplan.ArrayConceptQueryPlan;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.models.query.resultinfo.ResultInfoCollector;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Query type that combines a set of {@link ConceptQuery}s which are separately evaluated
 * and whose results are merged. If a SpecialDateUnion is required, the result will hold
 * the union of all dates from the separate queries.
 */
@Getter
@Setter
@CPSType(id = "ARRAY_CONCEPT_QUERY", base = QueryDescription.class)
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__(@JsonCreator))
public class ArrayConceptQuery extends Query {

	@NotEmpty @Valid
	private List<ConceptQuery> childQueries = new ArrayList<>();

	@NotNull
	protected DateAggregationMode dateAggregationMode = DateAggregationMode.NONE;


	@InternalOnly
	protected DateAggregationMode resolvedDateAggregationMode;

	public static ArrayConceptQuery createFromFeatures(List<CQElement> features) {
		List<ConceptQuery> cqWraps = features.stream()
											 .map(ConceptQuery::new)
											 .collect(Collectors.toList());
		return new ArrayConceptQuery(cqWraps);
	}

	public ArrayConceptQuery(@NonNull List<ConceptQuery> queries, @NonNull DateAggregationMode dateAggregationMode) {
		if(queries == null) {
			throw new IllegalArgumentException("No sub query list provided.");
		}
		this.childQueries = queries;
		this.dateAggregationMode = dateAggregationMode;
	}

	public ArrayConceptQuery( List<ConceptQuery> queries) {
		this(queries, DateAggregationMode.NONE);
	}

	@Override
	public void resolve(QueryResolveContext context) {
		resolvedDateAggregationMode = dateAggregationMode;
		if(context.getDateAggregationMode() != null) {
			log.trace("Overriding date aggregation mode ({}) with mode from context ({})", dateAggregationMode, context.getDateAggregationMode());
			resolvedDateAggregationMode = context.getDateAggregationMode();
		}
		childQueries.forEach(c -> c.resolve(context.withDateAggregationMode(resolvedDateAggregationMode)));
	}

	@Override
	public ArrayConceptQueryPlan createQueryPlan(QueryPlanContext context) {
		// Make sure the constructor and the adding is called with the same context.
		ArrayConceptQueryPlan aq = new ArrayConceptQueryPlan(!DateAggregationMode.NONE.equals(resolvedDateAggregationMode));
		aq.addChildPlans(childQueries, context);
		return aq;
	}

	@Override
	public void collectRequiredQueries(Set<ManagedExecution<?>> requiredQueries) {
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

		if(!DateAggregationMode.NONE.equals(getResolvedDateAggregationMode())){
			// Add one DateInfo for the whole Query
			collector.getInfos().add(0, dateInfo);
		}
	}

	@Override
	public void visit(Consumer<Visitable> visitor) {
		visitor.accept(this);
		childQueries.forEach(q -> q.visit(visitor));
	}
}
