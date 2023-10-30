package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Arrays;
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

	private final DateAggregationStep cteStep;

	public IntermediateTableCte(DateAggregationStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		List<SqlSelect> intermediateTableSelects = context.getSqlAggregationAction().getIntermediateTableSelects(context.getDateAggregationDates());
		Selects selects = Selects.builder()
								 .primaryColumn(context.getPrimaryColumn())
								 .sqlSelects(intermediateTableSelects)
								 .explicitSelects(context.getCarryThroughSelects())
								 .build();

		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		Field<Date>[] allStarts = dateAggregationDates.allStarts();
		Field<Date>[] allEnds = dateAggregationDates.allEnds();

		SqlFunctionProvider functionProvider = context.getFunctionProvider();
		Condition startBeforeEnd = functionProvider.greatest(allStarts).lessThan(functionProvider.least(allEnds));

		Condition startIsNull = Arrays.stream(allStarts)
									  .map(Field::isNull)
									  .reduce(Condition::or)
									  .orElseThrow();

		Condition intermediateTableCondition = startIsNull.orNot(startBeforeEnd);

		return QueryStep.builder()
						.selects(selects)
						.conditions(List.of(intermediateTableCondition));
	}

}
