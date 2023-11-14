
package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
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
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		List<SqlSelect> selects = context.getSqlAggregationAction().getIntermediateTableSelects(
				context.getDateAggregationDates(),
				context.getCarryThroughSelects()
		);
		Selects intermediateTableSelects = new Selects(
				context.getPrimaryColumn(),
				selects
		);

		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		List<Field<Date>> allStarts = dateAggregationDates.allStarts();
		List<Field<Date>> allEnds = dateAggregationDates.allEnds();

		SqlFunctionProvider functionProvider = context.getFunctionProvider();
		Condition startBeforeEnd = functionProvider.greatest(allStarts).lessThan(functionProvider.least(allEnds));

		Condition startIsNull = allStarts.stream()
										 .map(Field::isNull)
										 .reduce(Condition::or)
										 .orElseThrow();

		Condition intermediateTableCondition = startIsNull.orNot(startBeforeEnd);

		return QueryStep.builder()
						.selects(intermediateTableSelects)
						.conditions(List.of(intermediateTableCondition));
	}

}
