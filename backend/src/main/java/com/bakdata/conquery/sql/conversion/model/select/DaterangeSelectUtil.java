package com.bakdata.conquery.sql.conversion.model.select;

import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.EVENT_FILTER;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.INTERVAL_PACKING_SELECTS;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.UNNEST_DATE;
import static com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep.INTERVAL_COMPLETE;

import com.bakdata.conquery.models.datasets.concepts.DaterangeSelectOrFilter;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.FilterContext;
import com.bakdata.conquery.sql.conversion.cqelement.concept.IntervalPackingSelectsCte;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlIdColumns;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import com.bakdata.conquery.sql.conversion.model.filter.SqlFilters;
import com.bakdata.conquery.sql.conversion.model.filter.WhereClauses;
import com.bakdata.conquery.sql.conversion.model.filter.WhereCondition;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class DaterangeSelectUtil {

	@FunctionalInterface
	public interface AggregationFunction {
		FieldWrapper<?> apply(ColumnDateRange daterange, String alias, SqlFunctionProvider functionProvider);
	}

	@FunctionalInterface
	public interface FilterFunction {
		WhereCondition apply(Field<?> aggregationField);
	}

	/**
	 * Aggregates the daterange of a corresponding {@link DaterangeSelectOrFilter} and applies the respective converted aggregation via
	 * {@link IntervalPackingSelectsCte}s using additional predecessor tables.
	 */
	public static <S extends DaterangeSelectOrFilter & Named<?>> ConnectorSqlSelects createForSelect(
			S select,
			AggregationFunction aggregationFunction,
			SelectContext<ConnectorSqlTables> context
	) {
		String alias = context.getNameGenerator().selectName(select);
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();

		ColumnDateRange daterange = functionProvider.forArbitraryDateRange(select).as(alias);
		List<SqlSelect> rootSelects = daterange.toFields().stream()
											   .map(FieldWrapper::new)
											   .collect(Collectors.toList());

		SqlTables daterangeSelectTables = createTables(alias, context.getTables(), context);
		QueryStep lastIntervalPackingStep = applyIntervalPacking(daterange, daterangeSelectTables, context.getIds(), context.getTables(), context.getSqlDialect());

		ColumnDateRange qualified = daterange.qualify(daterangeSelectTables.getPredecessor(INTERVAL_PACKING_SELECTS));
		FieldWrapper<?> aggregationField = aggregationFunction.apply(qualified, alias, functionProvider);

		QueryStep intervalPackingSelectsStep = IntervalPackingSelectsCte.forSelect(
				lastIntervalPackingStep,
				qualified,
				aggregationField,
				daterangeSelectTables,
				context.getSqlDialect()
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
	 * {@link IntervalPackingSelectsCte}s using additional predecessor tables. Finally, the filter condition is created.
	 */
	public static <F extends DaterangeSelectOrFilter & Named<?>> SqlFilters createForFilter(
			F filter,
			AggregationFunction aggregationFunction,
			FilterFunction filterFunction,
			FilterContext<?> context
	) {
		String alias = context.getNameGenerator().selectName(filter);
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();

		ColumnDateRange daterange = functionProvider.forArbitraryDateRange(filter).as(alias);
		List<SqlSelect> rootSelects = daterange.toFields().stream()
				.map(FieldWrapper::new)
				.collect(Collectors.toList());

		SqlTables daterangeSelectTables = createTables(alias, context.getTables(), context);
		QueryStep lastIntervalPackingStep = applyIntervalPacking(daterange, daterangeSelectTables, context.getIds(), context.getTables(), context.getSqlDialect());

		ColumnDateRange qualified = daterange.qualify(daterangeSelectTables.getPredecessor(INTERVAL_PACKING_SELECTS));
		FieldWrapper<?> aggregationField = aggregationFunction.apply(qualified, alias, functionProvider);

		QueryStep intervalPackingSelectsStep = IntervalPackingSelectsCte.forSelect(
				lastIntervalPackingStep,
				qualified,
				aggregationField,
				daterangeSelectTables,
				context.getSqlDialect()
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
		Field<BigDecimal> durationSum = DSL.sum(
												   DSL.when(containsInfinityDate(validityDate, functionProvider), DSL.val(null, Integer.class))
													  .otherwise(dateDistanceInDays)
										   )
										   .as(alias);
		return new FieldWrapper<>(durationSum);
	}

	private static Condition containsInfinityDate(ColumnDateRange validityDate, SqlFunctionProvider functionProvider) {
		Field<Date> negativeInfinity = functionProvider.toDateField(functionProvider.getMinDateExpression());
		Field<Date> positiveInfinity = functionProvider.toDateField(functionProvider.getMaxDateExpression());
		return validityDate.getStart().eq(negativeInfinity).or(validityDate.getEnd().eq(positiveInfinity));
	}

	private static SqlTables createTables(String alias, ConnectorSqlTables connectorTables, Context context) {
		Map<CteStep, CteStep> predecessorMapping = new HashMap<>();
		String eventFilterCteName = connectorTables.cteName(EVENT_FILTER);
		predecessorMapping.putAll(IntervalPackingCteStep.getMappings(context.getSqlDialect()));
		if (context.getSqlDialect().supportsSingleColumnRanges()) {
			predecessorMapping.put(UNNEST_DATE, INTERVAL_COMPLETE);
			predecessorMapping.put(INTERVAL_PACKING_SELECTS, UNNEST_DATE);
		}
		else {
			predecessorMapping.put(INTERVAL_PACKING_SELECTS, INTERVAL_COMPLETE);
		}
		Map<CteStep, String> cteNameMap = CteStep.createCteNameMap(predecessorMapping.keySet(), alias, context.getNameGenerator());
		return new SqlTables(eventFilterCteName, cteNameMap, predecessorMapping);
	}

	private static QueryStep applyIntervalPacking(
			ColumnDateRange daterange,
			SqlTables dateUnionTables,
			SqlIdColumns idColumns,
			ConnectorSqlTables connectorSqlTables,
			SqlDialect sqlDialect
	) {
		String eventFilterCteName = connectorSqlTables.cteName(EVENT_FILTER);
		IntervalPackingContext intervalPackingContext = IntervalPackingContext.builder()
																			  .ids(idColumns.qualify(eventFilterCteName))
																			  .daterange(daterange.qualify(eventFilterCteName))
																			  .tables(dateUnionTables)
																			  .build();

		return sqlDialect.getIntervalPacker().aggregateAsArbitrarySelect(intervalPackingContext);
	}

}
