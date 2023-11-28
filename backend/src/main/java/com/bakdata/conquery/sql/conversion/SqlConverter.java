package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;

public class SqlConverter {

	private final NodeConversions nodeConversions;

	public SqlConverter(SqlDialect dialect, SqlConnectorConfig config) {
		this.nodeConversions = new NodeConversions(dialect, config);
	}

	public SqlQuery convert(QueryDescription queryDescription) {
		ConversionContext converted = nodeConversions.convert(queryDescription);
		return converted.getFinalQuery();
	}
}
