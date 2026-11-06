package com.bakdata.conquery.integration.sql.dialect;

import com.bakdata.conquery.sql.conversion.dialect.DialectBundle;

public interface TestDialectBundle extends DialectBundle {

	TestFunctionProvider getTestFunctionProvider();

}
