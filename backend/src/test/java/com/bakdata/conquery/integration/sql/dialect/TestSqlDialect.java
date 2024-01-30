package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;

public interface TestSqlDialect extends SqlDialect {

	TestFunctionProvider getTestFunctionProvider();

}
