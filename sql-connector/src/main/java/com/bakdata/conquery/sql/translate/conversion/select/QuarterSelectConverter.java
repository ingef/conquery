package com.bakdata.conquery.sql.translate.conversion.select;

import java.sql.Date;

import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.translate.ir.select.ColumnDateRange;
import com.bakdata.conquery.sql.translate.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.translate.query.concept.ConceptSqlSelects;
import com.bakdata.conquery.sql.translate.query.concept.ConnectorSqlSelects;
import lombok.experimental.UtilityClass;
import org.jooq.Field;

@UtilityClass
final class QuarterSelectConverter {

	static ConnectorSqlSelects connectorSelect(BuiltInSelects.Quarter select, SelectConversionContext context) {
		final FieldWrapper<String> quarter = quarter(select, context);
		final ExtractingSqlSelect<String> result = quarter.qualify(
				context.tables().getPredecessor(ConceptCteStep.AGGREGATION_FILTER));
		return ConnectorSqlSelects.builder().eventDateSelect(quarter).finalSelect(result).build();
	}

	static ConceptSqlSelects conceptSelect(BuiltInSelects.Quarter select, SelectConversionContext context) {
		final FieldWrapper<String> quarter = quarter(select, context);
		final ExtractingSqlSelect<String> result = quarter.qualify(
				context.tables().getPredecessor(ConceptCteStep.UNIVERSAL_SELECTS));
		return ConceptSqlSelects.builder().eventDateSelect(quarter).finalSelect(result).build();
	}

	private static FieldWrapper<String> quarter(BuiltInSelects.Quarter select, SelectConversionContext context) {
		final ColumnDateRange validityDate = context.validityDate().orElseThrow(
				() -> new IllegalArgumentException("Can't convert a quarter select without a validity date"));
		final String source = context.tables().getPredecessor(ConceptCteStep.INTERVAL_PACKING_SELECTS);
		final Field<Date> sampledDate = context.dialect().stratificationFunctions()
				.indexSelectorField(select.sample(), validityDate.qualify(source));
		return new FieldWrapper<>(context.dialect().yearQuarter(sampledDate).as(context.alias()));
	}
}
