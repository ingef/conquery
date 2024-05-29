package com.bakdata.conquery.sql.conversion.model.select;

import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.EVENT_FILTER;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.INTERVAL_PACKING_SELECTS;
import static com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep.UNNEST_DATE;
import static com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep.INTERVAL_COMPLETE;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.DaterangeSelect;
import com.bakdata.conquery.models.identifiable.Named;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.IntervalPackingSelectsCte;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingContext;
import com.bakdata.conquery.sql.conversion.cqelement.intervalpacking.IntervalPackingCteStep;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.bakdata.conquery.sql.conversion.model.CteStep;
import com.bakdata.conquery.sql.conversion.model.QueryStep;
import com.bakdata.conquery.sql.conversion.model.SqlTables;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

class DaterangeSelectUtil {

	@FunctionalInterface
	public interface AggregationFunction {
		FieldWrapper<?> apply(ColumnDateRange daterange, String alias, SqlFunctionProvider functionProvider);
	}

	/**
	 * Aggregates the daterange of a corresponding {@link DaterangeSelect} and applies the respective converted aggregation via
	 * {@link IntervalPackingSelectsCte}s using additional predecessor tables.
	 */
	public static <S extends DaterangeSelect & Named<?>> ConnectorSqlSelects createConnectorSqlSelects(
			S select,
			AggregationFunction aggregationFunction,
			SelectContext<Connector, ConnectorSqlTables> context
	) {
		String alias = context.getNameGenerator().selectName(select);
		ConnectorSqlTables tables = context.getTables();
		SqlFunctionProvider functionProvider = context.getSqlDialect().getFunctionProvider();

		ColumnDateRange daterange = functionProvider.forArbitraryDateRange(select).as(alias);
		List<SqlSelect> rootSelects = daterange.toFields().stream()
											   .map(FieldWrapper::new)
											   .collect(Collectors.toList());

		SqlTables daterangeSelectTables = createTables(alias, context);
		QueryStep lastIntervalPackingStep = applyIntervalPacking(daterange, daterangeSelectTables, context);

		ColumnDateRange qualified = daterange.qualify(daterangeSelectTables.getPredecessor(INTERVAL_PACKING_SELECTS));
		FieldWrapper<?> aggregationField = aggregationFunction.apply(qualified, alias, functionProvider);

		QueryStep intervalPackingSelectsStep = IntervalPackingSelectsCte.forSelect(
				lastIntervalPackingStep,
				qualified,
				aggregationField,
				daterangeSelectTables,
				context.getSqlDialect()
		);

		ExtractingSqlSelect<?> finalSelect = aggregationField.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelects(rootSelects)
								  .additionalPredecessor(Optional.of(intervalPackingSelectsStep))
								  .finalSelect(finalSelect)
								  .build();
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

	private static SqlTables createTables(String alias, SelectContext<Connector, ConnectorSqlTables> context) {
		Map<CteStep, CteStep> predecessorMapping = new HashMap<>();
		String eventFilterCteName = context.getTables().cteName(EVENT_FILTER);
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

	private static QueryStep applyIntervalPacking(ColumnDateRange daterange, SqlTables dateUnionTables, SelectContext<?, ?> context) {

		String eventFilterCteName = context.getTables().cteName(EVENT_FILTER);
		IntervalPackingContext intervalPackingContext = IntervalPackingContext.builder()
																			  .ids(context.getIds().qualify(eventFilterCteName))
																			  .daterange(daterange.qualify(eventFilterCteName))
																			  .tables(dateUnionTables)
																			  .build();

		return context.getSqlDialect()
					  .getIntervalPacker()
					  .aggregateAsArbitrarySelect(intervalPackingContext);
	}

}
