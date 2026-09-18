
package com.bakdata.conquery.sql.compiler.ir.aggregation;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.compiler.ir.DateAggregationDates;
import com.bakdata.conquery.sql.compiler.ir.FieldExpressions;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;

@Getter
class IntermediateTableCte extends DateAggregationCte {

	private final DateAggregationCteStep cteStep;

	public IntermediateTableCte(DateAggregationCteStep cteStep) {
		this.cteStep = cteStep;
	}

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
