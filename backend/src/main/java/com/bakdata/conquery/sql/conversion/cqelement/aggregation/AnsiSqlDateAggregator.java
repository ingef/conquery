package com.bakdata.conquery.sql.conversion.cqelement.aggregation;

import java.util.List;

import com.bakdata.conquery.models.query.queryplan.DateAggregationAction;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingTables;
import com.bakdata.conquery.sql.conversion.dialect.IntervalPacker;
import com.bakdata.conquery.sql.conversion.dialect.SqlDateAggregator;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.Selects;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.select.SqlSelect;

public class AnsiSqlDateAggregator implements SqlDateAggregator {

	private final SqlFunctionProvider functionProvider;
	private final IntervalPacker intervalPacker;

	public AnsiSqlDateAggregator(SqlFunctionProvider functionProvider, IntervalPacker intervalPacker) {
		this.functionProvider = functionProvider;
		this.intervalPacker = intervalPacker;
	}

	@Override
	public QueryStep apply(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction
	) {
		SqlAggregationAction aggregationAction = switch (dateAggregationAction) {
			case MERGE -> new MergeAggregateAction(joinedStep);
			case INTERSECT -> new IntersectAggregationAction(joinedStep);
			default -> throw new IllegalStateException("Unexpected date aggregation action: %s".formatted(dateAggregationAction));
		};

		DateAggregationContext context = DateAggregationContext.builder()
															   .sqlAggregationAction(aggregationAction)
															   .carryThroughSelects(carryThroughSelects)
															   .dateAggregationDates(dateAggregationDates)
															   .dateAggregationTables(aggregationAction.tableNames())
															   .primaryColumn(joinedStep.getQualifiedSelects().getPrimaryColumn())
															   .functionProvider(this.functionProvider)
															   .intervalPacker(this.intervalPacker)
															   .build();

		QueryStep finalDateAggregationStep = convertSteps(joinedStep, aggregationAction.dateAggregationCtes(), context);
		if (!aggregationAction.requiresIntervalPackingAfterwards()) {
			return finalDateAggregationStep;
		}

		Selects predecessorSelects = finalDateAggregationStep.getSelects();
		String joinedCteLabel = joinedStep.getCteName();
		SqlTables<IntervalPackingCteStep> intervalPackingTables =
				IntervalPackingTables.forGenericQueryStep(joinedCteLabel, finalDateAggregationStep);

		IntervalPackingContext intervalPackingContext =
				IntervalPackingContext.builder()
									  .nodeLabel(joinedCteLabel)
									  .primaryColumn(predecessorSelects.getPrimaryColumn())
									  .validityDate(predecessorSelects.getValidityDate().get())
									  .predecessor(finalDateAggregationStep)
									  .carryThroughSelects(carryThroughSelects)
									  .intervalPackingTables(intervalPackingTables)
									  .build();

		return this.intervalPacker.createIntervalPackingSteps(intervalPackingContext);
	}

	@Override
	public QueryStep invertAggregatedIntervals(QueryStep baseStep) {

		DateAggregationDates dateAggregationDates = DateAggregationDates.forSingleStep(baseStep);
		if (dateAggregationDates.dateAggregationImpossible()) {
			return baseStep;
		}

		Selects baseStepQualifiedSelects = baseStep.getQualifiedSelects();
		DateAggregationTables dateAggregationTables = InvertCteStep.createTableNames(baseStep);

		DateAggregationContext context = DateAggregationContext.builder()
															   .sqlAggregationAction(null) // when inverting, an aggregation has already been applied
															   .carryThroughSelects(baseStepQualifiedSelects.getSqlSelects())
															   .dateAggregationDates(dateAggregationDates)
															   .dateAggregationTables(dateAggregationTables)
															   .primaryColumn(baseStepQualifiedSelects.getPrimaryColumn())
															   .functionProvider(this.functionProvider)
															   .intervalPacker(this.intervalPacker)
															   .build();

		return convertSteps(baseStep, InvertCteStep.requiredSteps(), context);
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
