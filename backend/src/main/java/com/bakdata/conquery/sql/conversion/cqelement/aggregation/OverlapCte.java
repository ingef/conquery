package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.compiler.ir.DateAggregationDates;
import com.bakdata.conquery.sql.compiler.ir.FieldExpressions;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;

@Getter
class OverlapCte extends DateAggregationCte {

	private final DateAggregationCteStep cteStep;

	public OverlapCte(DateAggregationCteStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context, String predecessor) {

		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		List<Field<Date>> allStarts = dateAggregationDates.allStarts();
		List<Field<Date>> allEnds = dateAggregationDates.allEnds();

		ColumnDateRange overlapValidityDate = context.getSqlAggregationAction().getOverlapValidityDate(context.getDateAggregationDates());
		Selects overlapSelects = Selects.builder()
										.ids(context.getIds())
										.validityDate(Optional.of(overlapValidityDate.asValidityDateRange(predecessor)))
										.sqlSelects(context.getCarryThroughSelects())
										.build();

		Condition startBeforeEnd = FieldExpressions.greatest(allStarts).lessThan(FieldExpressions.least(allEnds));
		Condition allStartsNotNull = allStarts.stream()
											  .map(Field::isNotNull)
											  .reduce(Condition::and)
											  .orElseThrow();
		Condition overlapConditions = allStartsNotNull.and(startBeforeEnd);

		return QueryStep.builder()
						.selects(overlapSelects)
						.conditions(List.of(overlapConditions));
	}

}
