package com.bakdata.conquery.integration.sql;

import java.io.IOException;
import java.nio.file.Path;

import com.bakdata.conquery.integration.sql.dialect.TestSqlDialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.function.Executable;

@AllArgsConstructor
@Getter
public class SqlIntegrationTest implements Executable {

	public static final String SQL_TEST_DIR = "src/test/resources/tests/sql";

	private final SqlStandaloneSupport support;
	private final SqlIntegrationTestSpec testSpec;

	public void execute() throws IOException, JSONException {
		testSpec.importRequiredData(support);
		testSpec.executeTest(support);
	}

	public static SqlIntegrationTest fromPath(final Path path, final TestSqlDialect sqlDialect, final SqlConnectorConfig sqlConfig) {
		return new SqlIntegrationTest(new SqlStandaloneSupport(sqlDialect, sqlConfig), SqlIntegrationTestSpec.fromJsonSpec(path));
	}

}
