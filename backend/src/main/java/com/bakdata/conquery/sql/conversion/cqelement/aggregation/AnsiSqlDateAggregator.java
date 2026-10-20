package com.bakdata.conquery.sql.conversion.cqelement.aggregation;


import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;
import lombok.Data;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

@Data
public class AnsiSqlDateAggregator implements SqlDateAggregator {

	private final IntervalPacker intervalPacker;
	private final SqlFunctionProvider functionProvider;


	@Override
	public QueryStep apply(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			ConversionContext conversionContext
	) {
		return switch (dateAggregationAction) {
			case MERGE -> merge(joinedStep, carryThroughSelects, dateAggregationDates, conversionContext);
			case INTERSECT -> intersect(joinedStep, carryThroughSelects, dateAggregationDates);
			default -> throw new IllegalStateException("Unexpected date aggregation action: %s".formatted(dateAggregationAction));
		};
	}

	private QueryStep merge(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			ConversionContext conversionContext
	) {
		SqlTables dateAggregationTables = DateAggregationCteStep.createMergeTables(joinedStep, conversionContext.getNameGenerator());
		DateAggregationContext context =
				DateAggregationContext.builder()
									  .carryThroughSelects(carryThroughSelects)
									  .dateAggregationDates(dateAggregationDates)
									  .dateAggregationTables(dateAggregationTables)
									  .ids(joinedStep.getQualifiedSelects().getIds())
									  .conversionContext(conversionContext)
									  .build();

		QueryStep finalDateAggregationStep = convertSteps(
				joinedStep,
				List.of(new IntermediateTableCte(), new MergeCte()),
				context
		);

		Selects predecessorSelects = finalDateAggregationStep.getSelects();
		SqlTables intervalPackingTables = IntervalPackingCteStep.createTables(finalDateAggregationStep, context);

		IntervalPackingContext intervalPackingContext =
				IntervalPackingContext.builder()
									  .ids(predecessorSelects.getIds())
									  .daterange(predecessorSelects.getValidityDate().get())
									  .predecessor(finalDateAggregationStep)
									  .carryThroughSelects(carryThroughSelects)
									  .tables(intervalPackingTables)
									  .conversionContext(conversionContext)
									  .build();

		return this.intervalPacker.aggregateAsValidityDate(intervalPackingContext);
	}

	private QueryStep intersect(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates
	) {
		List<Field<Date>> allStarts = dateAggregationDates.allStarts();
		List<Field<Date>> allEnds = dateAggregationDates.allEnds();
		Condition overlapCondition = MergeCte.overlapCondition(allStarts, allEnds, this.functionProvider);

		Condition startIsNull = allStarts.stream()
										 .map(Field::isNull)
										 .reduce(Condition::or)
										 .orElseThrow();
		Condition startBeforeEnd = this.functionProvider.greatest(allStarts).lessThan(this.functionProvider.least(allEnds));
		Condition noOverlapCondition = startIsNull.orNot(startBeforeEnd);

		Field<Date> rangeStart = DSL.when(overlapCondition, this.functionProvider.greatest(allStarts))
										  .otherwise(DSL.inline(null, Date.class))
										  .as(DateAggregationCte.RANGE_START);
		Field<Date> rangeEnd = DSL.when(overlapCondition, this.functionProvider.least(allEnds))
										.otherwise(DSL.inline(null, Date.class))
										.as(DateAggregationCte.RANGE_END);
		ColumnDateRange intersectedValidityDate = ColumnDateRange.of(rangeStart, rangeEnd)
																		 .asValidityDateRange(joinedStep.getCteName());

		Selects intersectSelects = Selects.builder()
										.ids(joinedStep.getSelects().getIds())
										.validityDate(Optional.of(intersectedValidityDate))
										.sqlSelects(carryThroughSelects)
										.build();

		List<Condition> conditions = new ArrayList<>(joinedStep.getConditions());
		// Retain the former overlap/no-overlap union's three-valued-logic filtering for nullable end expressions.
		conditions.add(overlapCondition.or(noOverlapCondition));

		return joinedStep.toBuilder()
						 .selects(intersectSelects)
						 .conditions(conditions)
						 .build();
	}

	@Override
	public ColumnDateRange getAggregatedValidityDate(DateAggregationDates dateAggregationDates, DateAggregationAction dateAggregationAction) {
		//TODO(FK): i think this is only ever relevant with dateMode=Logical which i want to remove
		Field<Date> rangeStart = functionProvider.least(dateAggregationDates.allStarts());
		Field<Date> rangeEnd = functionProvider.greatest(dateAggregationDates.allEnds());

		return ColumnDateRange.of(
				rangeStart.as(DateAggregationCte.RANGE_START),
				rangeEnd.as(DateAggregationCte.RANGE_END)
		);
	}

	@Override
	public QueryStep invertAggregatedIntervals(QueryStep baseStep, ConversionContext conversionContext) {

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSingleStep(baseStep);
		if (dateAggregationDates.dateAggregationImpossible()) {
			return baseStep;
		}

		Selects baseStepQualifiedSelects = baseStep.getQualifiedSelects();
		SqlTables dateAggregationTables = DateAggregationCteStep.createInvertTables(baseStep, conversionContext.getNameGenerator());

		DateAggregationContext context = DateAggregationContext.builder()
															   .carryThroughSelects(baseStepQualifiedSelects.getSqlSelects())
															   .dateAggregationDates(dateAggregationDates)
															   .dateAggregationTables(dateAggregationTables)
															   .ids(baseStepQualifiedSelects.getIds())
															   .conversionContext(conversionContext)
															   .build();

		return convertSteps(baseStep, List.of(new RowNumberCte(), new InvertCte()), context);
	}

	private QueryStep convertSteps(QueryStep baseStep, List<DateAggregationCte> dateAggregationCTEs, DateAggregationContext context) {
		QueryStep finalDateAggregationStep = baseStep;
		for (DateAggregationCte step : dateAggregationCTEs) {
			finalDateAggregationStep = step.convert(context, finalDateAggregationStep);
			context = context.withStep(step.getCteStep(), finalDateAggregationStep);
		}
		return finalDateAggregationStep;
	}

}
