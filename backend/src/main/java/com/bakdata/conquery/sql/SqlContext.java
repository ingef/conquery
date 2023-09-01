package com.bakdata.conquery.sql;

import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.conversion.Context;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import lombok.Value;

@Value
public class SqlContext implements Context {
	SqlConnectorConfig config;
	SqlDialect sqlDialect;
}
