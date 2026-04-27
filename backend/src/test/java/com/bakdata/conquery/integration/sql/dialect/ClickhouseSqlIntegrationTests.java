package com.bakdata.conquery.integration.sql.dialect;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.ConqueryIntegrationTests;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.json.SqlTestDataImporter;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.mode.local.ManagedConnection;
import com.bakdata.conquery.models.config.DatabaseConnectionConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.sql.conversion.dialect.clickhouse.ClickhouseDialectBundle;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertValuesStepN;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class ClickhouseSqlIntegrationTests extends IntegrationTests {

	// SAP does not provide more than 1 image and on an update, the earlier image is deleted from dockerhub, thus latest tag is fine
	private final static DockerImageName IMAGE_TAGE = DockerImageName.parse("clickhouse/clickhouse-server");
	private static boolean useLocal = true;
	private static DSLContext dslContext;
	private static ManagedConnection managedConnection;

	static {
		final String raw = System.getenv("USE_LOCAL_CH_DB");
		if (!Strings.isNullOrEmpty(raw)) {
			useLocal = Boolean.parseBoolean(raw);
		}
	}

	public ClickhouseSqlIntegrationTests() {
		super(ConqueryIntegrationTests.DEFAULT_SQL_TEST_ROOT, "com.bakdata.conquery.integration");
	}


	@SneakyThrows
	@AfterAll
	public static void tearDownClass() {

	}

	@BeforeAll
	static void before() throws Exception {
		TestContextProvider provider = useLocal
									   ? new ClickhouseTestContainerContextProvider()
									   : new RemoteClickhouseContextProvider();

		log.info("Running Clickhouse tests with {}.", provider.getClass().getSimpleName());

		managedConnection = new ManagedConnection("test", provider.getSqlConnectorConfig(), provider.getDatabaseConfig(), null);
		managedConnection.start();

		dslContext = managedConnection.connect();
	}

	@AfterAll
	static void after() throws Exception {
		managedConnection.stop();
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicNode> sqlBackendTests() {

		DatabaseConnectionConfig databaseConfig = managedConnection.getConnection();
		TestDialectBundle testHanaDialect = new TestClickhouseDialectBundle();
		TestDataImporter testDataImporter = new SqlTestDataImporter(new CsvTableImporter(dslContext, testHanaDialect, databaseConfig));


		return Stream.concat(
				super.sqlProgrammaticTests(databaseConfig, managedConnection.getConfig(), testDataImporter),
				super.sqlQueryTests(databaseConfig, managedConnection.getConfig(), testDataImporter).stream()
		);
	}

	public static class TestClickhouseDialectBundle extends ClickhouseDialectBundle implements TestDialectBundle {

		public TestFunctionProvider getTestFunctionProvider() {
			return new ClickhouseTestFunctionProvider();
		}

	}

	private static class ClickhouseTestFunctionProvider implements TestFunctionProvider {

		@Override
		public void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement, DSLContext dslContext) {

			List<InsertValuesStepN> statements = new ArrayList<>();
			for (RowN rowN : content) {
				InsertValuesStepN<Record> values = dslContext.insertInto(table, columns)
															 .values(rowN);
				statements.add(values);
			}

			dslContext.batch(statements)
					  .execute();
		}

		@Override
		public String createDropTableStatement(Table<Record> table, DSLContext dslContext) {
			return dslContext.dropTable(table)
							 .getSQL(ParamType.INLINED);
		}

	}

	@Getter
	private static class ClickhouseTestContainerContextProvider implements TestContextProvider {

		private final DatabaseConnectionConfig databaseConfig;
		private final TestSqlConnectorConfig sqlConnectorConfig;

		@Container
		private final ClickHouseContainer container;

		public ClickhouseTestContainerContextProvider() {
			this.container = new ClickHouseContainer(IMAGE_TAGE);
			this.container.start();

			this.databaseConfig = DatabaseConnectionConfig.builder()
														  .dialect(Dialect.CLICKHOUSE)
														  .jdbcConnectionUrl(container.getJdbcUrl())
														  .databaseUsername(container.getUsername())
														  .databasePassword(container.getPassword())
														  .build();
			this.sqlConnectorConfig = new TestSqlConnectorConfig(databaseConfig);
		}
	}

	@Getter
	private static class RemoteClickhouseContextProvider implements TestContextProvider {

		private final static String PORT = Objects.requireNonNullElse(System.getenv("CONQUERY_SQL_PORT"), "8123");
		private final static String HOST = System.getenv("CONQUERY_SQL_DB");
		private final static String CONNECTION_URL = "jdbc:clickhouse://%s:%s/".formatted(HOST, PORT);
		private final static String USERNAME = Objects.requireNonNullElse(System.getenv("CONQUERY_SQL_USER"), "default");
		private final static String PASSWORD = System.getenv("CONQUERY_SQL_PASSWORD");

		private final DatabaseConnectionConfig databaseConfig;
		private final TestSqlConnectorConfig sqlConnectorConfig;

		public RemoteClickhouseContextProvider() {
			this.databaseConfig = DatabaseConnectionConfig.builder()
														  .dialect(Dialect.CLICKHOUSE)
														  .jdbcConnectionUrl(CONNECTION_URL)
														  .databaseUsername(USERNAME)
														  .databasePassword(PASSWORD)
														  .build();
			this.sqlConnectorConfig = new TestSqlConnectorConfig(databaseConfig);
		}

	}

}
