package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.RandomValueSelect;
import com.bakdata.conquery.sql.compiler.ir.concept.ConnectorSqlSelects;



import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.model.operation.BuiltInSelects;

public class RandomValueSelectConverter implements SelectConverter<RandomValueSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(RandomValueSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return ResolvedSelectAdapter.values(select, BuiltInSelects.ValueOperation.RANDOM, selectContext);
	}
}