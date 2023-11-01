package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import lombok.Getter;
import org.jooq.Condition;
import org.jooq.Field;

@Getter
class OverlapCte extends DateAggregationCte {

	private final DateAggregationStep cteStep;

	public OverlapCte(DateAggregationStep cteStep) {
		this.cteStep = cteStep;
	}

	@Override
	protected QueryStep.QueryStepBuilder convertStep(DateAggregationContext context) {

		DateAggregationDates dateAggregationDates = context.getDateAggregationDates();
		List<Field<Date>> allStarts = dateAggregationDates.allStarts();
		List<Field<Date>> allEnds = dateAggregationDates.allEnds();

		ColumnDateRange overlapValidityDate = context.getSqlAggregationAction().getOverlapValidityDate(
				context.getDateAggregationDates(),
				context.getFunctionProvider()
		);
		Selects overlapSelects = new Selects(
				context.getPrimaryColumn(),
				Optional.of(overlapValidityDate),
				context.getCarryThroughSelects()
		);

		SqlFunctionProvider functionProvider = context.getFunctionProvider();
		Condition startBeforeEnd = functionProvider.greatest(allStarts).lessThan(functionProvider.least(allEnds));
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
