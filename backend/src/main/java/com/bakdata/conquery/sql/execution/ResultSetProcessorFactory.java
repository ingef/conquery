package com.bakdata.conquery.sql.execution;

import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;

public class ResultSetProcessorFactory {

	public static ResultSetProcessor create(SqlDialect sqlDialect) {
		return new DefaultResultSetProcessor(sqlDialect.getCDateSetParser());
	}

}
