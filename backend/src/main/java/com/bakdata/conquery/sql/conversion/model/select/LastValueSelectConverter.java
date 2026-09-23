package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;

public class LastValueSelectConverter implements SelectConverter<LastValueSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(LastValueSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return ResolvedSelectAdapter.values(select, BuiltInSelects.ValueOperation.LAST, selectContext);
	}
}