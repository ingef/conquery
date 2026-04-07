package com.bakdata.conquery.sql.execution;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;

public class ResultSetProcessorFactory {

	public static ResultSetProcessor create(ConqueryConfig config, DialectBundle dialectBundle) {
		return new DefaultResultSetProcessor(config, dialectBundle.getCDateSetParser());
	}

}
