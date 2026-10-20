
package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import org.jooq.Condition;
import org.jooq.Field;

class IntermediateTableCte extends DateAggregationCte {

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context, String predecessor) {

		List<SqlSelect> intermediateTableSelects = Stream.of(
													  context.getDateAggregationDates().allStartsAndEnds(),
													  context.getCarryThroughSelects()
											  )
											  .flatMap(Collection::stream)
											  .collect(Collectors.toList());
		Selects selects = Selects.builder()
								 .ids(context.getIds())
								 .sqlSelects(intermediateTableSelects)
								 .build();

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
						.selects(selects)
						.fromTable(QueryStep.toTableLike(predecessor))
						.conditions(List.of(intermediateTableCondition));
	}

	@Override
	public DateAggregationCteStep getCteStep() {
		return DateAggregationCteStep.INTERMEDIATE_TABLE;
	}

}
