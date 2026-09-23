package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.DistinctSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;

public class DistinctSelectConverter implements SelectConverter<DistinctSelect> {
	@Override
	public ConnectorSqlSelects connectorSelect(DistinctSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return ResolvedSelectAdapter.values(select, BuiltInSelects.ValueOperation.DISTINCT, selectContext);
	}
}
