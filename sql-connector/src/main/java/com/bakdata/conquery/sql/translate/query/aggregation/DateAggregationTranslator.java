package com.bakdata.conquery.sql.translate.query.aggregation;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.models.query.DateAggregationAction;
import com.bakdata.conquery.sql.translate.dialect.TranslatorDialect;
import com.bakdata.conquery.sql.translate.ir.FieldExpressions;
import com.bakdata.conquery.sql.translate.ir.QueryStep;
import com.bakdata.conquery.sql.translate.ir.Selects;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.SqlSelect;
import com.bakdata.conquery.sql.translate.naming.SqlNameGenerator;
import com.bakdata.conquery.sql.translate.query.interval.AnsiSqlIntervalPacker;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingContext;
import com.bakdata.conquery.sql.translate.query.interval.IntervalPackingCteStep;
import lombok.experimental.UtilityClass;
import org.jooq.Field;

/** Builds date aggregation and inversion query-step graphs from connector IR. */
@UtilityClass
public class DateAggregationTranslator {

	public static QueryStep aggregate(
			QueryStep joinedStep,
			List<SqlSelect> carryThroughSelects,
			DateAggregationDates dateAggregationDates,
			DateAggregationAction dateAggregationAction,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
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
				.dateAggregationTables(aggregationAction.tableNames(nameGenerator))
				.ids(joinedStep.getQualifiedSelects().getIds())
				.translatorDialect(translatorDialect)
				.build();

		QueryStep finalDateAggregationStep = convertSteps(joinedStep, aggregationAction.dateAggregationCtes(), context);
		if (!aggregationAction.requiresIntervalPackingAfterwards()) {
			return finalDateAggregationStep;
		}

		Selects predecessorSelects = finalDateAggregationStep.getSelects();
		SqlTables intervalPackingTables = IntervalPackingCteStep.createTables(
				finalDateAggregationStep,
				translatorDialect,
				nameGenerator
		);
		IntervalPackingContext intervalPackingContext = IntervalPackingContext.builder()
				.ids(predecessorSelects.getIds())
				.daterange(predecessorSelects.getValidityDate().orElseThrow())
				.predecessor(Optional.of(finalDateAggregationStep))
				.carryThroughSelects(carryThroughSelects)
				.tables(intervalPackingTables)
				.build();

		return AnsiSqlIntervalPacker.aggregateAsValidityDate(intervalPackingContext);
	}

	public static ColumnDateRange getAggregatedValidityDate(DateAggregationDates dateAggregationDates) {
		Field<Date> rangeStart = FieldExpressions.least(dateAggregationDates.allStarts());
		Field<Date> rangeEnd = FieldExpressions.greatest(dateAggregationDates.allEnds());

		return ColumnDateRange.of(
				rangeStart.as(DateAggregationCte.RANGE_START),
				rangeEnd.as(DateAggregationCte.RANGE_END)
		);
	}

	public static QueryStep invert(
			QueryStep baseStep,
			TranslatorDialect translatorDialect,
			SqlNameGenerator nameGenerator
	) {
		DateAggregationDates dateAggregationDates = DateAggregationDates.forSingleStep(baseStep);
		if (dateAggregationDates.dateAggregationImpossible()) {
			return baseStep;
		}

		Selects baseStepQualifiedSelects = baseStep.getQualifiedSelects();
		SqlTables dateAggregationTables = DateAggregationCteStep.createInvertTables(baseStep, nameGenerator);
		DateAggregationContext context = DateAggregationContext.builder()
				.sqlAggregationAction(null)
				.carryThroughSelects(baseStepQualifiedSelects.getSqlSelects())
				.dateAggregationDates(dateAggregationDates)
				.dateAggregationTables(dateAggregationTables)
				.ids(baseStepQualifiedSelects.getIds())
				.translatorDialect(translatorDialect)
				.build();

		return convertSteps(baseStep, DateAggregationCteStep.createInvertCtes(), context);
	}

	private static QueryStep convertSteps(
			QueryStep baseStep,
			List<DateAggregationCte> dateAggregationCtes,
			DateAggregationContext context
	) {
		QueryStep finalDateAggregationStep = baseStep;
		for (DateAggregationCte step : dateAggregationCtes) {
			finalDateAggregationStep = step.convert(context, finalDateAggregationStep);
			context = context.withStep(step.getCteStep(), finalDateAggregationStep);
		}
		return finalDateAggregationStep;
	}
}
