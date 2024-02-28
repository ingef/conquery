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
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conversion.dialect.HanaSqlDialect;
import com.bakdata.conquery.sql.conversion.supplier.DateNowSupplier;
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

	private final static DockerImageName HANA_IMAGE = DockerImageName.parse("saplabs/hanaexpress:2.00.072.00.20231123.1");
	private static final Path TMP_HANA_MOUNT_DIR = Paths.get("/tmp/data/hana");
	private static boolean useLocalHanaDb = true;

	static {
		final String USE_LOCAL_HANA_DB = System.getenv("USE_LOCAL_HANA_DB");
		if (!Strings.isNullOrEmpty(USE_LOCAL_HANA_DB)) {
			useLocalHanaDb = Boolean.parseBoolean(USE_LOCAL_HANA_DB);
		}
	}

	public HanaSqlIntegrationTests() {
		super(ConqueryIntegrationTests.DEFAULT_SQL_TEST_ROOT, "com.bakdata.conquery.integration");
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicNode> sqlBackendTests() {

		TestContextProvider provider = useLocalHanaDb
									   ? new HanaTestcontainerContextProvider()
									   : new RemoteHanaContextProvider();

		log.info("Running HANA tests with %s.".formatted(provider.getClass().getSimpleName()));

		DSLContext dslContext = provider.getDslContext();
		SqlConnectorConfig config = provider.getSqlConnectorConfig();
		TestHanaDialect testHanaDialect = new TestHanaDialect(dslContext);
		TestDataImporter testDataImporter = new SqlTestDataImporter(new CsvTableImporter(dslContext, testHanaDialect, config));

		return Stream.concat(
				super.sqlProgrammaticTests(config, testDataImporter),
				super.sqlQueryTests(config, testDataImporter).stream()
		);
	}

	@SneakyThrows
	@BeforeAll
	public static void prepareTmpHanaDir() {

		if (!useLocalHanaDb) {
			return;
		}

		Path masterPasswordFile = TMP_HANA_MOUNT_DIR.resolve("password.json");
		String content = "{\"master_password\":\"%s\"}".formatted(HanaContainer.DEFAULT_MASTER_PASSWORD);

		Files.createDirectories(TMP_HANA_MOUNT_DIR);
		Files.write(masterPasswordFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		Files.setPosixFilePermissions(TMP_HANA_MOUNT_DIR, Set.of(PosixFilePermission.values()));
	}

	@SneakyThrows
	@AfterAll
	public static void tearDownClass() {
		if (!Files.exists(TMP_HANA_MOUNT_DIR)) {
			return;
		}
		try (Stream<Path> walk = Files.walk(TMP_HANA_MOUNT_DIR)) {
			walk.sorted(Comparator.naturalOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		}
	}

	public static class TestHanaDialect extends HanaSqlDialect implements TestSqlDialect {

		public static final MockDateNowSupplier DATE_NOW_SUPPLIER = new MockDateNowSupplier();


		public TestHanaDialect(DSLContext dslContext) {
			super(dslContext);
		}

		@Override
		public DateNowSupplier getDateNowSupplier() {
			return DATE_NOW_SUPPLIER;
		}

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

		private final DSLContext dslContext;
		private final SqlConnectorConfig sqlConnectorConfig;

		@Container
		private final HanaContainer<?> hanaContainer;

		public HanaTestcontainerContextProvider() {
			this.hanaContainer = new HanaContainer<>(HANA_IMAGE)
					.withFileSystemBind(TMP_HANA_MOUNT_DIR.toString(), "/home/secrets");
			this.hanaContainer.start();

			this.sqlConnectorConfig = SqlConnectorConfig.builder()
														.enabled(true)
														.dialect(Dialect.HANA)
														.withPrettyPrinting(true)
														.jdbcConnectionUrl(hanaContainer.getJdbcUrl())
														.databaseUsername(hanaContainer.getUsername())
														.databasePassword(hanaContainer.getPassword())
														.primaryColumn("pid")
														.build();
			this.dslContext = DslContextFactory.create(sqlConnectorConfig);
		}

	}

	@Getter
	private static class RemoteHanaContextProvider implements TestContextProvider {

		private final static String PORT = Objects.requireNonNullElse(System.getenv("CONQUERY_SQL_PORT"), "39041");
		private final static String HOST = System.getenv("CONQUERY_SQL_DB");
		private final static String CONNECTION_URL = "jdbc:sap://%s:%s/databaseName=HXE&encrypt=true&validateCertificate=false".formatted(HOST, PORT);
		private final static String USERNAME = System.getenv("CONQUERY_SQL_USER");
		private final static String PASSWORD = System.getenv("CONQUERY_SQL_PASSWORD");
		private final DSLContext dslContext;
		private final SqlConnectorConfig sqlConnectorConfig;

		public RemoteHanaContextProvider() {
			this.sqlConnectorConfig = SqlConnectorConfig.builder()
														.enabled(true)
														.dialect(Dialect.HANA)
														.withPrettyPrinting(true)
														.jdbcConnectionUrl(CONNECTION_URL)
														.databaseUsername(USERNAME)
														.databasePassword(PASSWORD)
														.primaryColumn("pid")
														.build();
			this.dslContext = DslContextFactory.create(sqlConnectorConfig);
		}

	}

}
