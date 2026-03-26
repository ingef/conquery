package com.bakdata.conquery.integration.sql.dialect;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.ConqueryIntegrationTests;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.json.SqlTestDataImporter;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.integration.sql.CsvTableImporter;
import com.bakdata.conquery.integration.sql.testcontainer.hana.HanaContainer;
import com.bakdata.conquery.mode.local.ManagedConnection;
import com.bakdata.conquery.models.config.DatabaseConnectionConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.sql.conversion.dialect.HanaDialectBundle;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class HanaSqlIntegrationTests extends IntegrationTests {

	// SAP does not provide more than 1 image and on an update, the earlier image is deleted from dockerhub, thus latest tag is fine
	private final static DockerImageName HANA_IMAGE = DockerImageName.parse("saplabs/hanaexpress:latest");
	private static final Path TMP_HANA_MOUNT_DIR = Paths.get("/tmp/data/hana");
	private static boolean useLocalHanaDb = true;
	private static DSLContext dslContext;
	private static ManagedConnection managedConnection;

	static {
		final String USE_LOCAL_HANA_DB = System.getenv("USE_LOCAL_HANA_DB");
		if (!Strings.isNullOrEmpty(USE_LOCAL_HANA_DB)) {
			useLocalHanaDb = Boolean.parseBoolean(USE_LOCAL_HANA_DB);
		}
	}

	public HanaSqlIntegrationTests() {
		super(ConqueryIntegrationTests.DEFAULT_SQL_TEST_ROOT, "com.bakdata.conquery.integration");
	}

	@SneakyThrows
	@BeforeAll
	static void before() throws Exception {
		if (useLocalHanaDb) {
			Path masterPasswordFile = TMP_HANA_MOUNT_DIR.resolve("password.json");
			String content = "{\"master_password\":\"%s\"}".formatted(HanaContainer.DEFAULT_MASTER_PASSWORD);

			Files.createDirectories(TMP_HANA_MOUNT_DIR);
			Files.write(masterPasswordFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			Files.setPosixFilePermissions(TMP_HANA_MOUNT_DIR, Set.of(PosixFilePermission.values()));
		}


		TestContextProvider provider = useLocalHanaDb
									   ? new HanaTestcontainerContextProvider()
									   : new RemoteHanaContextProvider();

		log.info("Running HANA tests with {}.", provider.getClass().getSimpleName());

		managedConnection = new ManagedConnection("test", provider.getSqlConnectorConfig(), provider.getDatabaseConfig(), null);
		managedConnection.start();

		dslContext = managedConnection.connect();
	}

	@AfterAll
	static void after() throws Exception {
		managedConnection.stop();

		if (Files.exists(TMP_HANA_MOUNT_DIR)) {
			try (Stream<Path> walk = Files.walk(TMP_HANA_MOUNT_DIR)) {
				walk.sorted(Comparator.naturalOrder())
					.map(Path::toFile)
					.forEach(File::delete);
			}
		}
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicNode> sqlBackendTests() {

		DatabaseConnectionConfig databaseConfig = managedConnection.getConnection();
		TestHanaDialectBundle testHanaDialect = new TestHanaDialectBundle();
		TestDataImporter testDataImporter = new SqlTestDataImporter(new CsvTableImporter(dslContext, testHanaDialect, databaseConfig));


		return Stream.concat(
				super.sqlProgrammaticTests(databaseConfig, managedConnection.getConfig(), testDataImporter),
				super.sqlQueryTests(databaseConfig, managedConnection.getConfig(), testDataImporter).stream()
		);
	}

	public static class TestHanaDialectBundle extends HanaDialectBundle implements TestDialectBundle {

		public TestFunctionProvider getTestFunctionProvider() {
			return new HanaTestFunctionProvider();
		}

	}

	private static class HanaTestFunctionProvider implements TestFunctionProvider {

		@Override
		public void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement, DSLContext dslContext)
				throws SQLException {
			for (RowN rowN : content) {
				String insertRowStatement = dslContext.insertInto(table, columns)
													  .values(rowN)
													  .getSQL(ParamType.INLINED);

				statement.execute(insertRowStatement);
			}
		}

		@Override
		public String createDropTableStatement(Table<Record> table, DSLContext dslContext) {
			return dslContext.dropTable(table)
							 .getSQL(ParamType.INLINED);
		}

	}

	@Getter
	private static class HanaTestcontainerContextProvider implements TestContextProvider {

		private final DatabaseConnectionConfig databaseConfig;
		private final TestSqlConnectorConfig sqlConnectorConfig;

		@Container
		private final HanaContainer<?> hanaContainer;

		public HanaTestcontainerContextProvider() {
			this.hanaContainer = new HanaContainer<>(HANA_IMAGE)
					.withFileSystemBind(TMP_HANA_MOUNT_DIR.toString(), "/home/secrets");
			this.hanaContainer.start();


			this.databaseConfig = DatabaseConnectionConfig.builder()
														  .dialect(Dialect.HANA)
														  .jdbcConnectionUrl(hanaContainer.getJdbcUrl())
														  .databaseUsername(hanaContainer.getUsername())
														  .databasePassword(hanaContainer.getPassword())
														  .build();
			this.sqlConnectorConfig = new TestSqlConnectorConfig(databaseConfig);
		}
	}

	@Getter
	private static class RemoteHanaContextProvider implements TestContextProvider {

		private final static String PORT = Objects.requireNonNullElse(System.getenv("CONQUERY_SQL_PORT"), "39041");
		private final static String HOST = System.getenv("CONQUERY_SQL_DB");
		private final static String CONNECTION_URL = "jdbc:sap://%s:%s/databaseName=HXE&encrypt=true&validateCertificate=false".formatted(HOST, PORT);
		private final static String USERNAME = Objects.requireNonNullElse(System.getenv("CONQUERY_SQL_USER"), "SYSTEM");
		private final static String PASSWORD = System.getenv("CONQUERY_SQL_PASSWORD");
		private final DatabaseConnectionConfig databaseConfig;
		private final TestSqlConnectorConfig sqlConnectorConfig;

		public RemoteHanaContextProvider() {
			this.databaseConfig = DatabaseConnectionConfig.builder()
														  .dialect(Dialect.HANA)
														  .jdbcConnectionUrl(CONNECTION_URL)
														  .databaseUsername(USERNAME)
														  .databasePassword(PASSWORD)
														  .build();
			this.sqlConnectorConfig = new TestSqlConnectorConfig(databaseConfig);
		}

	}

}
