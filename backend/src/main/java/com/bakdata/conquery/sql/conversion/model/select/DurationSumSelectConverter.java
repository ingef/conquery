package com.bakdata.conquery.sql.conversion.model.select;

import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.models.datasets.concepts.select.connector.specific.DurationSumSelect;
import com.bakdata.conquery.sql.conversion.cqelement.concept.ConnectorSqlTables;
import com.bakdata.conquery.sql.conversion.model.ColumnDateRange;

public class DurationSumSelectConverter implements SelectConverter<DurationSumSelect> {

	@Override
	public ConnectorSqlSelects connectorSelect(DurationSumSelect select, SelectContext<Connector, ConnectorSqlTables> selectContext) {
		return DaterangeSelectUtil.createConnectorSqlSelects(
				select,
				(daterange, alias, functionProvider) -> {
					ColumnDateRange asDualColumn = functionProvider.toDualColumn(daterange);
					return DaterangeSelectUtil.createDurationSumSqlSelect(alias, asDualColumn, functionProvider);
				},
				selectContext
		);
	}

}
