package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.models.config.SqlConnectorConfig;
import org.jooq.DSLContext;

public interface TestContextProvider {

	SqlConnectorConfig getSqlConnectorConfig();

	DSLContext getDslContext();

}
