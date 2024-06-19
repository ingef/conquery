package com.bakdata.conquery.sql.conversion.model.select;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.temporal.ChronoUnit;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.concept.specific.EventDurationSumSelect;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptCteStep;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConceptSqlTables;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;
import com.google.common.base.Preconditions;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class EventDurationSumSelectConverter implements SelectConverter<EventDurationSumSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(EventDurationSumSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {

		FieldWrapper<BigDecimal> stringAggregation = createEventDurationSumAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .eventDateSelect(stringAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}

	@Override
	public ConceptSqlSelects conceptSelect(EventDurationSumSelect select, SelectContext<TreeConcept, ConceptSqlTables> selectContext) {

		FieldWrapper<BigDecimal> stringAggregation = createEventDurationSumAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.getTables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));

		return ConceptSqlSelects.builder()
								.eventDateSelect(stringAggregation)
								.finalSelect(finalSelect)
								.build();
	}

	private FieldWrapper<BigDecimal> createEventDurationSumAggregation(EventDurationSumSelect select, SelectContext<?, ?> selectContext) {

		Preconditions.checkArgument(selectContext.getValidityDate().isPresent(), "Can't convert an EventDateUnionSelect without a validity date being present");
		String predecessorCteName = selectContext.getTables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS);
		ColumnDateRange qualified = selectContext.getValidityDate().get().qualify(predecessorCteName);
		ColumnDateRange asDualColumn = selectContext.getFunctionProvider().toDualColumn(qualified);

		SqlFunctionProvider functionProvider = selectContext.getFunctionProvider();
		String alias = selectContext.getNameGenerator().selectName(select);

		Field<BigDecimal> durationSum = DSL.sum(
												   DSL.when(containsInfinityDate(asDualColumn, functionProvider), DSL.val(null, Integer.class))
													  .otherwise(functionProvider.dateDistance(ChronoUnit.DAYS, asDualColumn.getStart(), asDualColumn.getEnd()))
										   )
										   .as(alias);
		return new FieldWrapper<>(durationSum);
	}

	private static Condition containsInfinityDate(ColumnDateRange validityDate, SqlFunctionProvider functionProvider) {
		Field<Date> negativeInfinity = functionProvider.toDateField(functionProvider.getMinDateExpression());
		Field<Date> positiveInfinity = functionProvider.toDateField(functionProvider.getMaxDateExpression());
		return validityDate.getStart().eq(negativeInfinity).or(validityDate.getEnd().eq(positiveInfinity));
	}

}
