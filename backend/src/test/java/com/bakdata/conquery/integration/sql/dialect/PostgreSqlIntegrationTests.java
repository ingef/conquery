package com.bakdata.conquery.integration.sql.dialect;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.SqlQuery;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.DateDistanceSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlDialect;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
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
	private static DSLContext dslContext;
	private static SqlConnectorConfig sqlConfig;

	public PostgreSqlIntegrationTests() {
		super("tests/", "com.bakdata.conquery.integration");
	}

	@Container
	private static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER = new PostgreSQLContainer<>(postgreSqlImageName)
			.withDatabaseName(DATABASE_NAME)
			.withUsername(USERNAME)
			.withPassword(PASSWORD);


	@BeforeAll
	static void before() {
		POSTGRESQL_CONTAINER.start();
		sqlConfig = SqlConnectorConfig.builder()
									  .dialect(Dialect.POSTGRESQL)
									  .jdbcConnectionUrl(POSTGRESQL_CONTAINER.getJdbcUrl())
									  .databaseUsername(USERNAME)
									  .databasePassword(PASSWORD)
									  .withPrettyPrinting(true)
									  .primaryColumn("pid")
									  .build();
		dslContext = DslContextFactory.create(sqlConfig);
	}

	@Test
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public void shouldThrowException() {
		// This can be removed as soon as we switch to a full integration test including the REST API
		I18n.init();
		SqlExecutionService executionService = new SqlExecutionService(dslContext);
		SqlManagedQuery validQuery = new SqlManagedQuery(new ConceptQuery(), null, null, null, new SqlQuery("SELECT 1"));
		Assertions.assertThatNoException().isThrownBy(() -> executionService.execute(validQuery));

		// executing an empty query should throw an SQL error
		SqlManagedQuery emptyQuery = new SqlManagedQuery(new ConceptQuery(), null, null, null, new SqlQuery(""));
		Assertions.assertThatThrownBy(() -> executionService.execute(emptyQuery))
				  .isInstanceOf(ConqueryError.SqlError.class)
				  .hasMessageContaining("$org.postgresql.util.PSQLException");
	}


	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicTest> sqlBackendTests() {
		return super.sqlTests(new TestPostgreSqlDialect(dslContext), sqlConfig);
	}

	private static class TestPostgreSqlDialect extends PostgreSqlDialect implements TestSqlDialect {

		public TestPostgreSqlDialect(DSLContext dslContext) {
			super(dslContext);
		}

		@Override
		public List<SelectConverter<? extends Select>> getSelectConverters() {
			return this.customizeSelectConverters(List.of(
					new DateDistanceSelectConverter(new MockDateNowSupplier())
			));
		}

		public TestFunctionProvider getTestFunctionProvider() {
			return new PostgreSqlTestFunctionProvider();
		}
	}

	private static class PostgreSqlTestFunctionProvider implements TestFunctionProvider {

	}

}
