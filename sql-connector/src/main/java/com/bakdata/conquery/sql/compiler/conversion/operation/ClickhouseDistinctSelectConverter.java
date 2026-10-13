package com.bakdata.conquery.sql.compiler.conversion.operation;

import static org.jooq.impl.DSL.field;

import com.bakdata.conquery.sql.compiler.ir.SqlTables;
import com.bakdata.conquery.sql.compiler.ir.concept.ConceptCteStep;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.compiler.ir.select.SingleColumnSqlSelect;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;

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
