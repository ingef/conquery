package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;

import java.sql.Date;
import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import lombok.Data;
import org.jooq.Field;

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
		SqlAggregationAction aggregationAction = switch (dateAggregationAction) {
			case MERGE -> new MergeAggregateAction(joinedStep);
			case INTERSECT -> new IntersectAggregationAction(joinedStep);
			default -> throw new IllegalStateException("Unexpected date aggregation action: %s".formatted(dateAggregationAction));
		};

		DateAggregationContext context =
				DateAggregationContext.builder()
									  .sqlAggregationAction(aggregationAction)
									  .carryThroughSelects(carryThroughSelects)
									  .dateAggregationDates(dateAggregationDates)
									  .dateAggregationTables(aggregationAction.tableNames(conversionContext.getNameGenerator()))
									  .ids(joinedStep.getQualifiedSelects().getIds())
									  .conversionContext(conversionContext)
									  .build();

		QueryStep finalDateAggregationStep = convertSteps(joinedStep, aggregationAction.dateAggregationCtes(), context);
		if (!aggregationAction.requiresIntervalPackingAfterwards()) {
			return finalDateAggregationStep;
		}

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
															   .sqlAggregationAction(null) // when inverting, an aggregation has already been applied
															   .carryThroughSelects(baseStepQualifiedSelects.getSqlSelects())
															   .dateAggregationDates(dateAggregationDates)
															   .dateAggregationTables(dateAggregationTables)
															   .ids(baseStepQualifiedSelects.getIds())
															   .conversionContext(conversionContext)
															   .build();

		return convertSteps(baseStep, DateAggregationCteStep.createInvertCtes(), context);
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
