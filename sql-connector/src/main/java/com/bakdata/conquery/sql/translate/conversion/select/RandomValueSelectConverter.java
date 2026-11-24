package com.bakdata.conquery.sql.translate.conversion.select;

import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.ExtractingSqlSelect;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.translate.query.concept.ConnectorSqlSelects;
import org.jooq.Field;

final class RandomValueSelectConverter {


	static ConnectorSqlSelects connectorSelect(BuiltInSelects.Values select, SelectConversionContext selectContext) {

		SqlTables tables = selectContext.tables();

		String rootTableName = tables.getRootTable();
		String columnName = select.column().physicalName();
		ExtractingSqlSelect<?> rootSelect = new ExtractingSqlSelect<>(rootTableName, columnName, Object.class);

		String alias = selectContext.alias();
		Field<?> qualifiedRootSelect = rootSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_SELECT)).select();
		Field<?> firstAggregation = selectContext.dialect().random(qualifiedRootSelect).as(alias);
		FieldWrapper<?> firstAggregationSqlSelect = new FieldWrapper<>(firstAggregation, columnName);

		ExtractingSqlSelect<?> finalSelect = firstAggregationSqlSelect.qualify(tables.getPredecessor(ConceptCteStep.AGGREGATION_FILTER));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(rootSelect)
								  .aggregationSelect(firstAggregationSqlSelect)
								  .finalSelect(finalSelect)
								  .build();
	}
}
