package com.bakdata.conquery.integration.sql.dialect;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.integration.sql.testcontainer.hana.HanaContainer;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conversion.dialect.HanaSqlDialect;
import com.bakdata.conquery.sql.conversion.select.DateDistanceConverter;
import com.bakdata.conquery.sql.conversion.select.SelectConverter;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class HanaSqlIntegrationTests extends IntegrationTests {

	private boolean useLocalHanaDb = true;

	public HanaSqlIntegrationTests() {
		super("tests/", "com.bakdata.conquery.integration");

		final String USE_LOCAL_HANA_DB = System.getenv("USE_LOCAL_HANA_DB");
		if (!Strings.isNullOrEmpty(USE_LOCAL_HANA_DB)) {
			useLocalHanaDb = Boolean.parseBoolean(USE_LOCAL_HANA_DB);
		}
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicTest> sqlBackendTests() {

		TestContextProvider provider = useLocalHanaDb
									   ? new HanaTestcontainerContextProvider()
									   : new RemoteHanaContextProvider();

		log.info("Running HANA tests with %s.".formatted(provider.getClass().getSimpleName()));

		DSLContext dslContext = provider.getDslContext();
		SqlConnectorConfig config = provider.getSqlConnectorConfig();

		return super.sqlTests(new TestHanaDialect(dslContext), config);
	}

	private static class TestHanaDialect extends HanaSqlDialect {

		public TestHanaDialect(DSLContext dslContext) {
			super(dslContext);
		}

		@Override
		public List<SelectConverter<? extends Select>> getSelectConverters() {
			return this.customizeSelectConverters(List.of(
					new DateDistanceConverter(new MockDateNowSupplier())
			));
		}

	}

	@Getter
	private static class HanaTestcontainerContextProvider implements TestContextProvider {

		private final static DockerImageName HANA_IMAGE = DockerImageName.parse("saplabs/hanaexpress:2.00.061.00.20220519.1");
		private final DSLContext dslContext;
		private final SqlConnectorConfig sqlConnectorConfig;

		@Container
		private final HanaContainer<?> hanaContainer;

		public HanaTestcontainerContextProvider() {
			this.hanaContainer = new HanaContainer<>(HANA_IMAGE);
			this.hanaContainer.start();

			this.sqlConnectorConfig = SqlConnectorConfig.builder()
														.dialect(Dialect.HANA)
														.jdbcConnectionUrl(hanaContainer.getJdbcUrl())
														.databaseUsername(hanaContainer.getUsername())
														.databasePassword(hanaContainer.getPassword())
														.withPrettyPrinting(true)
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
