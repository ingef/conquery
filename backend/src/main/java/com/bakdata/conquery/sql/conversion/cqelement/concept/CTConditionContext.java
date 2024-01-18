package com.bakdata.conquery.sql.conversion.cqelement.concept;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Connector;
import com.bakdata.conquery.sql.conversion.dialect.SqlFunctionProvider;
import lombok.Value;

@Value
public class CTConditionContext {

	Table connectorTable;
	Column connectorColumn;
	SqlFunctionProvider functionProvider;

	public static CTConditionContext create(Connector connector, SqlFunctionProvider functionProvider) {
		return new CTConditionContext(
				connector.getTable(),
				connector.getColumn(),
				functionProvider
		);
	}

}
