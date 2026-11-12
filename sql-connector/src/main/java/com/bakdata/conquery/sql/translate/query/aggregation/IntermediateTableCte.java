
package com.bakdata.conquery.sql.translate.query.aggregation;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.translate.ir.FieldExpressions;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.Field;

@Getter
@RequiredArgsConstructor
class IntermediateTableCte extends DateAggregationCte {

	private final DateAggregationCteStep cteStep;

	@Override
	protected QueryStep convertStep(DateAggregationContext context, String predecessor, QueryStep previous) {


		List<SqlSelect> intermediateTableSelects = context.getSqlAggregationAction().getIntermediateTableSelects(
				context.getDateAggregationDates(),
				context.getCarryThroughSelects()
		);
		Selects selects = Selects.builder()
								 .ids(context.getIds())
								 .sqlSelects(intermediateTableSelects)
								 .build();

		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		List<Field<Date>> allStarts = dateAggregationDates.allStarts();
		List<Field<Date>> allEnds = dateAggregationDates.allEnds();

		Condition startBeforeEnd = FieldExpressions.greatest(allStarts).lessThan(FieldExpressions.least(allEnds));

		Condition startIsNull = allStarts.stream()
										 .map(Field::isNull)
										 .reduce(Condition::or)
										 .orElseThrow();

		Condition intermediateTableCondition = startIsNull.orNot(startBeforeEnd);

		return QueryStep.builder()
						.cteName(context.getDateAggregationTables().cteName(getCteStep()))
						.selects(selects)
						.fromTable(QueryStep.toTableLike(predecessor))
						.conditions(List.of(intermediateTableCondition))
						.predecessors(List.of(previous))
						.build();
	}

}
