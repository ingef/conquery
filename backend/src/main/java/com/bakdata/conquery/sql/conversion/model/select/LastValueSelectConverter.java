package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.connector.LastValueSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;

public class LastValueSelectConverter implements SelectConverter<LastValueSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(LastValueSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {
		return ValueSelectUtil.createValueSelect(
				select.getColumn().resolve(),
				selectContext.getNameGenerator().selectName(select),
				(valueField, orderByFields) -> selectContext.getFunctionProvider().last(valueField, orderByFields),
				selectContext
		);
	}
}
