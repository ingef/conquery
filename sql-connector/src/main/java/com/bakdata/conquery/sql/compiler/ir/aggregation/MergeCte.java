package com.bakdata.conquery.sql.compiler.ir.aggregation;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import lombok.Getter;

@Getter
class MergeCte extends DateAggregationCte {

	private final DateAggregationCteStep cteStep;

	public MergeCte(DateAggregationCteStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep convertStep(DateAggregationContext context, String predecessor, QueryStep previous) {

		SqlAggregationAction aggregationAction = context.getSqlAggregationAction();
		List<QueryStep> noOverlapSteps = aggregationAction.getNoOverlapSelects(context);
		QueryStep overlapStep = aggregationAction.getOverlapStep(context);

		List<QueryStep> unionSteps = noOverlapSteps.stream().map(MergeCte::createUnionStep).collect(Collectors.toList());

		return QueryStep.builder()
						.cteName(context.getDateAggregationTables().cteName(getCteStep()))
						.selects(overlapStep.getQualifiedSelects())
						.fromTable(QueryStep.toTableLike(predecessor))
						.union(unionSteps)
						.predecessors(List.of(previous))
						.build();
	}

	private static QueryStep createUnionStep(QueryStep noOverlapStep) {
		return QueryStep.builder()
						.selects(noOverlapStep.getQualifiedSelects())
						.fromTable(QueryStep.toTableLike(noOverlapStep.getCteName()))
						.build();
	}

}
