package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.conversion.model.QueryStep;
import lombok.Getter;

@Getter
class MergeCte extends DateAggregationCte {

	private final DateAggregationCteStep cteStep;

	public MergeCte(DateAggregationCteStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		SqlAggregationAction aggregationAction = context.getSqlAggregationAction();
		List<QueryStep> noOverlapSteps = aggregationAction.getNoOverlapSelects(context);
		QueryStep overlapStep = aggregationAction.getOverlapStep(context);

		List<QueryStep> unionSteps = noOverlapSteps.stream().map(MergeCte::createUnionStep).collect(Collectors.toList());

		return QueryStep.builder()
						.selects(overlapStep.getQualifiedSelects())
						.union(unionSteps);
	}

	private static QueryStep createUnionStep(QueryStep noOverlapStep) {
		return QueryStep.builder()
						.selects(noOverlapStep.getQualifiedSelects())
						.fromTable(QueryStep.toTableLike(noOverlapStep.getCteName()))
						.build();
	}

}
