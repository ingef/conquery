package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateUnionSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.compiler.ir.select.FieldWrapper;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;

public class DateUnionSelectConverter implements SelectConverter<DateUnionSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(DateUnionSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return ResolvedSelectAdapter.connectorSelect(new com.bakdata.conquery.sql.model.operation.BuiltInSelects.DateUnion(
				select.getName(), com.bakdata.conquery.sql.conversion.model.EntitySchemaAdapter.from(select)), select.getName(), selectContext);
	}

}
