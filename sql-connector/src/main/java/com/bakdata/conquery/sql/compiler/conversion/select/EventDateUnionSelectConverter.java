package com.bakdata.conquery.sql.compiler.conversion.select;

import com.bakdata.conquery.sql.compiler.dialect.CompilerDialect;
import com.bakdata.conquery.sql.compiler.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.compiler.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.query.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.compiler.query.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;


public class EventDateUnionSelectConverter {


	public ConnectorSqlSelects connectorSelect(BuiltInSelects.EventDateUnion select, SelectConversionContext selectContext) {

		FieldWrapper<?> stringAggregation = createEventDateUnionAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.tables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .eventDateSelect(stringAggregation)
								  .finalSelect(finalSelect)
								  .build();
	}


	public ConceptSqlSelects conceptSelect(BuiltInSelects.EventDateUnion select, SelectConversionContext selectContext) {

		FieldWrapper<?> stringAggregation = createEventDateUnionAggregation(select, selectContext);
		ExtractingSqlSelect<?> finalSelect = stringAggregation.qualify(selectContext.tables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));

		return ConceptSqlSelects.builder()
								.eventDateSelect(stringAggregation)
								.finalSelect(finalSelect)
								.build();
	}

	private static FieldWrapper<?> createEventDateUnionAggregation(BuiltInSelects.EventDateUnion select, SelectConversionContext selectContext) {

		if (selectContext.validityDate().isEmpty()) { throw new IllegalArgumentException("Can't convert an EventDateUnionSelect without a validity date being present"); }
		ColumnDateRange validityDate = selectContext.validityDate().get();

		CompilerDialect functionProvider = selectContext.dialect();
		String alias = selectContext.alias();

		ColumnDateRange qualified = validityDate.qualify(selectContext.tables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS));
		return new FieldWrapper<>(functionProvider.aggregateDateRanges(qualified.getStart(), qualified.getEnd()).as(alias));
	}

}
