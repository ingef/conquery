package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

public class AnsiSqlDateAggregator implements SqlDateAggregator {

	private final IntervalPacker intervalPacker;

	public AnsiSqlDateAggregator(IntervalPacker intervalPacker) {
		this.intervalPacker = intervalPacker;
	}

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
