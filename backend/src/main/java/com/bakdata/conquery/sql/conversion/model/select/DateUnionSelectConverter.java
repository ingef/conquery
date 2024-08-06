package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DateUnionSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;

public class DateUnionSelectConverter implements SelectConverter<DateUnionSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(DateUnionSelect select, SelectContext<ConnectorSqlTables> selectContext) {
		return DaterangeSelectUtil.createConnectorSqlSelects(
				select,
				(daterange, alias, functionProvider) -> new FieldWrapper<>(functionProvider.daterangeStringAggregation(daterange).as(alias)),
				selectContext
		);
	}

}
