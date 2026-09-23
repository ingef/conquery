package com.bakdata.conquery.sql.compiler.conversion.operation;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;


import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class EventDurationSumSelectConverter {


	public ConnectorSqlSelects connectorSelect(BuiltInSelects.EventDurationSum select, SelectConversionContext selectContext) {

		FieldWrapper<BigDecimal> stringAggregation = createEventDurationSumAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.tables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .eventDateSelect(stringAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}


	public ConceptSqlSelects conceptSelect(BuiltInSelects.EventDurationSum select, SelectConversionContext selectContext) {

		FieldWrapper<BigDecimal> stringAggregation = createEventDurationSumAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.tables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));

		return ConceptSqlSelects.builder()
								.eventDateSelect(stringAggregation)
								.finalSelect(finalSelect)
								.build();
	}

	private FieldWrapper<BigDecimal> createEventDurationSumAggregation(BuiltInSelects.EventDurationSum select, SelectConversionContext selectContext) {

		if (selectContext.validityDate().isEmpty()) { throw new IllegalArgumentException("Can't convert an EventDateUnionSelect without a validity date being present"); }
		String predecessorCteName = selectContext.tables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS);
		ColumnDateRange qualified = selectContext.validityDate().get().qualify(predecessorCteName);
		ColumnDateRange asDualColumn = selectContext.dialect().toDualColumn(qualified);

		CompilerDialect functionProvider = selectContext.dialect();
		String alias = selectContext.alias();

		Field<BigDecimal> durationSum = DSL.sum(
												   DSL.when(containsInfinityDate(asDualColumn, functionProvider), DSL.inline(null, Integer.class))
													  .otherwise(functionProvider.dateDistance(ChronoUnit.DAYS, asDualColumn.getStart(), asDualColumn.getEnd()))
										   )
										   .as(alias);
		return new FieldWrapper<>(durationSum);
	}

	private static Condition containsInfinityDate(ColumnDateRange validityDate, CompilerDialect functionProvider) {
		Field<Date> negativeInfinity = functionProvider.minimumDate();
		Field<Date> positiveInfinity = functionProvider.maximumDate();
		return validityDate.getStart().eq(negativeInfinity).or(validityDate.getEnd().eq(positiveInfinity));
	}

}
