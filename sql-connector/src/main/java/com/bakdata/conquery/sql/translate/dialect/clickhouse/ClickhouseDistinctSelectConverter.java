package com.bakdata.conquery.sql.translate.dialect.clickhouse;

import static org.jooq.impl.DSL.field;

import com.bakdata.conquery.sql.model.operation.BuiltInSelects;
import com.bakdata.conquery.sql.translate.conversion.select.SelectConversionContext;
import com.bakdata.conquery.sql.translate.conversion.select.SubstringSelect;
import com.bakdata.conquery.sql.translate.ir.SqlTables;
import com.bakdata.conquery.sql.translate.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.translate.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.translate.query.concept.ConceptCteStep;
import com.bakdata.conquery.sql.translate.query.concept.ConnectorSqlSelects;

public class ClickhouseDistinctSelectConverter {



	public static ConnectorSqlSelects connectorSelect(BuiltInSelects.Values distinctSelect, SelectConversionContext selectContext) {

		String alias = selectContext.alias();

		SqlTables tables = selectContext.tables();
		SingleColumnSqlSelect preprocessingSelect =
				SubstringSelect.getSubstringSelect(distinctSelect.column(), distinctSelect.substring(), selectContext.tables().getRootTable(), alias);

		String preprocessingTable = selectContext.tables().cteName(ConceptCteStep.PREPROCESSING);
		SingleColumnSqlSelect qualified = preprocessingSelect.qualify(preprocessingTable);

		FieldWrapper<?> grouped =
				new FieldWrapper<>(field("arrayFilter(x -> x <> '' and x is not null, groupUniqArray({0}))", Object.class, qualified.select()).as(alias),
								   qualified.select().getName()
				);

		SingleColumnSqlSelect finalSelect = grouped.qualify(tables.cteName(ConceptCteStep.AGGREGATION_SELECT));

		return ConnectorSqlSelects.builder()
								  .preprocessingSelect(preprocessingSelect)
								  .aggregationSelect(grouped)
								  .finalSelect(finalSelect)
								  .build();
	}


}
