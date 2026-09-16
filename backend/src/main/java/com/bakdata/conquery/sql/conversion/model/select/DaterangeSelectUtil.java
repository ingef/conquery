package com.bakdata.conquery.sql.conversion.model.select;

import static com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep.PREPROCESSING;
import static org.jooq.impl.DSL.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.identifiable.LabeledNamespaceIdentifiable;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SqlSelect;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectPreparation;
import com.bakdata.conquery.sql.compiler.ir.interval.IntervalPackingSelectCompiler;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.QueryStep;
import com.bakdata.conquery.sql.compiler.ir.SqlIdColumns;
import com.bakdata.conquery.sql.compiler.ir.concept.SqlFilters;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereClauses;
import com.bakdata.conquery.sql.compiler.ir.condition.WhereCondition;
import org.jooq.Condition;
import org.jooq.Field;

public class DaterangeSelectUtil {

	/**
	 * Aggregates the daterange of a corresponding {@link DaterangeSelectOrFilter} and applies the respective converted aggregation via
	 * {@link IntervalPackingSelectCompiler} using an additional predecessor table.
	 */
	public static <S extends Select & DaterangeSelectOrFilter> ConnectorSqlSelects createForSelect(
			S select,
			AggregationFunction aggregationFunction,
			SelectContext<ConnectorSqlTables> context
	) {
		String alias = context.getNameGenerator().legacyOperationName(select.getName());
		SqlFunctionProvider functionProvider = context.getFunctionProvider();

		ColumnDateRange daterange = functionProvider.forArbitraryDateRange(select).as(alias);
		List<SqlSelect> rootSelects = daterange.toFields().stream()
											   .map(FieldWrapper::new)
											   .collect(Collectors.toList());

		IntervalPackingSelectPreparation preparation = prepareIntervalSelect(
				alias,
				daterange,
				context.getIds(),
				context.getTables(),
				context
		);
		ColumnDateRange qualified = preparation.dateRange();
		FieldWrapper<?> aggregationField = aggregationFunction.apply(qualified, alias, functionProvider);

		QueryStep intervalPackingSelectsStep = IntervalPackingSelectCompiler.compileArbitrarySelect(
				preparation.predecessor(),
				qualified,
				aggregationField,
				preparation.tables(),
				context.getCompilerDialect()
		);

		ConnectorSqlTables tables = context.getTables();
		ExtractingSqlSelect<?> finalSelect = aggregationField.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(rootSelects)
								  .additionalPredecessor(Optional.of(intervalPackingSelectsStep))
								  .finalSelect(finalSelect)
								  .build();
	}

	/**
	 * Aggregates the daterange of a corresponding {@link DaterangeSelectOrFilter} and applies the respective converted aggregation via
	 * {@link IntervalPackingSelectCompiler} using an additional predecessor table. Finally, the filter condition is created.
	 */
	public static SqlFilters createForFilter(
			DaterangeSelectOrFilter filter,
			AggregationFunction aggregationFunction,
			Function<Field<?>, WhereCondition> filterFunction,
			FilterContext<?> context
	) {
		String alias = context.getNameGenerator().legacyOperationName(((LabeledNamespaceIdentifiable<?>) filter).getName());
		SqlFunctionProvider functionProvider = context.getCompilerDialect().getFunctionProvider();

		ColumnDateRange daterange = functionProvider.forArbitraryDateRange(filter).as(alias);
		List<SqlSelect> rootSelects = daterange.toFields().stream()
											   .map(FieldWrapper::new)
											   .collect(Collectors.toList());

		IntervalPackingSelectPreparation preparation = prepareIntervalSelect(
				alias,
				daterange,
				context.getIds(),
				context.getTables(),
				context
		);
		ColumnDateRange qualified = preparation.dateRange();
		FieldWrapper<?> aggregationField = aggregationFunction.apply(qualified, alias, functionProvider);

		QueryStep intervalPackingSelectsStep = IntervalPackingSelectCompiler.compileArbitrarySelect(
				preparation.predecessor(),
				qualified,
				aggregationField,
				preparation.tables(),
				context.getCompilerDialect()
		);

		ConnectorSqlSelects sqlSelects = ConnectorSqlSelects.builder()
															.preprocessingSelects(rootSelects)
															.additionalPredecessor(Optional.of(intervalPackingSelectsStep))
															.build();

		ConnectorSqlTables tables = context.getTables();
		Field<?> qualifiedAggregationField = aggregationField.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER)).select();
		WhereClauses whereClauses = WhereClauses.builder().groupFilter(filterFunction.apply(qualifiedAggregationField)).build();

		return new SqlFilters(sqlSelects, whereClauses);
	}

	public static FieldWrapper<BigDecimal> createDurationSumSqlSelect(String alias, ColumnDateRange validityDate, SqlFunctionProvider functionProvider) {
		Field<Integer> dateDistanceInDays = functionProvider.dateDistance(ChronoUnit.DAYS, validityDate.getStart(), validityDate.getEnd());
		Field<BigDecimal> durationSum = sum(when(containsInfinityDate(validityDate, functionProvider), inline(null, Integer.class))
													.otherwise(dateDistanceInDays)
		)
				.as(alias);
		return new FieldWrapper<>(durationSum);
	}

	private static Condition containsInfinityDate(ColumnDateRange validityDate, SqlFunctionProvider functionProvider) {
		Field<Date> negativeInfinity = functionProvider.getMinDateExpression();
		Field<Date> positiveInfinity = functionProvider.getMaxDateExpression();

		return validityDate.getStart().eq(negativeInfinity).or(validityDate.getEnd().eq(positiveInfinity));
	}

	private static IntervalPackingSelectPreparation prepareIntervalSelect(
			String alias,
			ColumnDateRange daterange,
			SqlIdColumns ids,
			ConnectorSqlTables tables,
			Context context
	) {
		return IntervalPackingSelectCompiler.prepareArbitrarySelect(
				alias,
				tables.cteName(PREPROCESSING),
				ids,
				daterange,
				context.getCompilerDialect(),
				context.getNameGenerator()
		);
	}

	@FunctionalInterface
	public interface AggregationFunction {
		FieldWrapper<?> apply(ColumnDateRange daterange, String alias, SqlFunctionProvider functionProvider);
	}

}
