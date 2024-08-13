package com.bakdata.conquery.integration.sql.dialect;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.ConqueryIntegrationTests;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.json.SqlTestDataImporter;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.sql.DSLContextWrapper;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.bakdata.conquery.sql.execution.ResultSetProcessorFactory;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@Slf4j
public class PostgreSqlIntegrationTests extends IntegrationTests {

	private static final DockerImageName postgreSqlImageName = DockerImageName.parse("postgres:alpine3.17");
	private static final String DATABASE_NAME = "test";
	private static final String USERNAME = "user";
	private static final String PASSWORD = "pass";

	private static DSLContextWrapper dslContextWrapper;
	private static DatabaseConfig databaseConfig;
	private static TestSqlConnectorConfig sqlConfig;
	private static TestSqlDialect testSqlDialect;
	private static SqlTestDataImporter testDataImporter;
	private static boolean useLocalPostgresDb = true;

	static {
		final String USE_LOCAL_POSTGRES_DB = System.getenv("USE_LOCAL_POSTGRES_DB");
		if (!Strings.isNullOrEmpty(USE_LOCAL_POSTGRES_DB)) {
			useLocalPostgresDb = Boolean.parseBoolean(USE_LOCAL_POSTGRES_DB);
		}
	}

	public PostgreSqlIntegrationTests() {
		super(ConqueryIntegrationTests.DEFAULT_SQL_TEST_ROOT, "com.bakdata.conquery.integration");
	}



	@BeforeAll
	static void before() {
		TestContextProvider provider = useLocalPostgresDb
									   ? new PortgresTestcontainerContextProvider()
									   : new RemotePostgresContextProvider();

		databaseConfig = provider.getDatabaseConfig();
		sqlConfig = new TestSqlConnectorConfig(databaseConfig);
		dslContextWrapper = DslContextFactory.create(databaseConfig, sqlConfig, null);
		testSqlDialect = new TestPostgreSqlDialect();
		testDataImporter = new SqlTestDataImporter(new CsvTableImporter(dslContextWrapper.getDslContext(), testSqlDialect, databaseConfig));
	}

	@AfterAll
	static void after() throws IOException {
		dslContextWrapper.close();
	}

	@Test
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public void shouldThrowException() {
		// This can be removed as soon as we switch to a full integration test including the REST API
		I18n.init();
		ResultSetProcessor resultSetProcessor = ResultSetProcessorFactory.create(config, testSqlDialect);
		SqlExecutionService executionService = new SqlExecutionService(dslContextWrapper.getDslContext(), resultSetProcessor);
		SqlQuery validQuery = new TestSqlQuery("SELECT 1");
		Assertions.assertThatNoException().isThrownBy(() -> executionService.execute(validQuery));

		// executing an empty query should throw an SQL error
		SqlQuery emptyQuery = new TestSqlQuery("");
		Assertions.assertThatThrownBy(() -> executionService.execute(emptyQuery))
				  .isInstanceOf(ConqueryError.SqlError.class)
				  .hasMessageContaining("$org.postgresql.util.PSQLException");
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicNode> sqlBackendTests() {
		return Stream.concat(
				super.sqlProgrammaticTests(databaseConfig, sqlConfig, testDataImporter),
				super.sqlQueryTests(databaseConfig, sqlConfig, testDataImporter).stream()
		);
	}

	public static class TestPostgreSqlDialect extends PostgreSqlDialect implements TestSqlDialect {

		public static final MockDateNowSupplier DATE_NOW_SUPPLIER = new MockDateNowSupplier();

		@Override
		public DateNowSupplier getDateNowSupplier() {
			return DATE_NOW_SUPPLIER;
		}

		public TestFunctionProvider getTestFunctionProvider() {
			return new PostgreSqlTestFunctionProvider();
		}
	}

	private static class PostgreSqlTestFunctionProvider implements TestFunctionProvider {

	}

	@Getter
	private static class TestSqlQuery extends SqlQuery {
		protected TestSqlQuery(String sql) {
			super(sql);
		}
	}

	@Getter
	private static class PortgresTestcontainerContextProvider implements TestContextProvider {

		private final DSLContextWrapper dslContextWrapper;
		private final DatabaseConfig databaseConfig;
		private final TestSqlConnectorConfig sqlConnectorConfig;

		@Container
		private final PostgreSQLContainer<?> postgreSQLContainer;

		public PortgresTestcontainerContextProvider() {
			this.postgreSQLContainer = new PostgreSQLContainer<>(postgreSqlImageName)
					.withDatabaseName(DATABASE_NAME)
					.withUsername(USERNAME)
					.withPassword(PASSWORD);
			this.postgreSQLContainer.start();
			this.databaseConfig = DatabaseConfig.builder()
										   .dialect(Dialect.POSTGRESQL)
										   .jdbcConnectionUrl(postgreSQLContainer.getJdbcUrl())
										   .databaseUsername(USERNAME)
										   .databasePassword(PASSWORD)
										   .build();

			this.sqlConnectorConfig = new TestSqlConnectorConfig(databaseConfig);
			this.dslContextWrapper = DslContextFactory.create(this.databaseConfig, sqlConnectorConfig, null);
		}

	}


	@Getter
	private static class RemotePostgresContextProvider implements TestContextProvider {

		private final static String PORT = Objects.requireNonNullElse(System.getenv("CONQUERY_SQL_PORT"), "39041");
		private final static String HOST = System.getenv("CONQUERY_SQL_HOST");
		private final static String DATABASE = System.getenv("CONQUERY_SQL_DATABASE");
		private final static String CONNECTION_URL = "jdbc:postgresql://%s:%s/%s".formatted(HOST, PORT, DATABASE);
		private final static String USERNAME = System.getenv("CONQUERY_SQL_USER");
		private final static String PASSWORD = System.getenv("CONQUERY_SQL_PASSWORD");
		private final DSLContextWrapper dslContextWrapper;
		private final DatabaseConfig databaseConfig;
		private final TestSqlConnectorConfig sqlConnectorConfig;

		public RemotePostgresContextProvider() {
			this.databaseConfig = DatabaseConfig.builder()
												.dialect(Dialect.POSTGRESQL)
												.jdbcConnectionUrl(CONNECTION_URL)
												.databaseUsername(USERNAME)
												.databasePassword(PASSWORD)
												.build();
			this.sqlConnectorConfig = new TestSqlConnectorConfig(databaseConfig);
			this.dslContextWrapper = DslContextFactory.create(databaseConfig, sqlConnectorConfig, null);
		}

	}
}
