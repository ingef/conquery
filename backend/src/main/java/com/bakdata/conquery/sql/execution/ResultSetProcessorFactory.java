package com.bakdata.conquery.sql.execution;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;

public class ResultSetProcessorFactory {

	public static ResultSetProcessor create(ConqueryConfig config, SqlDialect sqlDialect) {
		return new DefaultResultSetProcessor(config, sqlDialect.getCDateSetParser());
	}

}
