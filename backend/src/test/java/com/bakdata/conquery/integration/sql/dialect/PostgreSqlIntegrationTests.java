package com.bakdata.conquery.integration.sql.dialect;

import java.util.Collections;
import java.util.List;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.integration.ConqueryIntegrationTests;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.models.i18n.I18n;
import com.bakdata.conquery.models.query.resultinfo.ResultInfo;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.DateDistanceFilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.filter.FilterConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.DateDistanceSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.dialect.PostgreSqlDialect;
import com.bakdata.conquery.sql.conversion.model.SqlQuery;
import com.bakdata.conquery.sql.execution.ResultSetProcessorFactory;
import com.bakdata.conquery.sql.execution.SqlExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.jooq.DSLContext;
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
	private static DSLContext dslContext;
	private static SqlConnectorConfig sqlConfig;
	private static TestSqlDialect testSqlDialect;

	public PostgreSqlIntegrationTests() {
		super(ConqueryIntegrationTests.DEFAULT_SQL_TEST_ROOT, "com.bakdata.conquery.integration");
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
									  .enabled(true)
									  .dialect(Dialect.POSTGRESQL)
									  .jdbcConnectionUrl(POSTGRESQL_CONTAINER.getJdbcUrl())
									  .databaseUsername(USERNAME)
									  .databasePassword(PASSWORD)
									  .withPrettyPrinting(true)
									  .primaryColumn("pid")
									  .build();
		dslContext = DslContextFactory.create(sqlConfig);
		testSqlDialect = new TestPostgreSqlDialect(dslContext);
	}

	@Test
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public void shouldThrowException() {
		// This can be removed as soon as we switch to a full integration test including the REST API
		I18n.init();
		SqlExecutionService executionService = new SqlExecutionService(dslContext, ResultSetProcessorFactory.create(testSqlDialect));
		SqlManagedQuery validQuery = new SqlManagedQuery(new ConceptQuery(), null, null, null, toSqlQuery("SELECT 1"));
		Assertions.assertThatNoException().isThrownBy(() -> executionService.execute(validQuery));

		// executing an empty query should throw an SQL error
		SqlManagedQuery emptyQuery = new SqlManagedQuery(new ConceptQuery(), null, null, null, toSqlQuery(""));
		Assertions.assertThatThrownBy(() -> executionService.execute(emptyQuery))
				  .isInstanceOf(ConqueryError.SqlError.class)
				  .hasMessageContaining("$org.postgresql.util.PSQLException");
	}


	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public List<DynamicNode> sqlBackendTests() {
		return super.sqlTests(testSqlDialect, sqlConfig);
	}

	public static class TestPostgreSqlDialect extends PostgreSqlDialect implements TestSqlDialect {

		public static final MockDateNowSupplier DATE_NOW_SUPPLIER = new MockDateNowSupplier();


		public TestPostgreSqlDialect(DSLContext dslContext) {
			super(dslContext);
		}

		@Override
		public List<SelectConverter<? extends Select>> getSelectConverters() {
			return this.customizeSelectConverters(List.of(
					new DateDistanceSelectConverter(DATE_NOW_SUPPLIER)
			));
		}

		@Override
		public List<FilterConverter<?, ?>> getFilterConverters() {
			return this.customizeFilterConverters(List.of(
					new DateDistanceFilterConverter(DATE_NOW_SUPPLIER)
			));
		}

		public TestFunctionProvider getTestFunctionProvider() {
			return new PostgreSqlTestFunctionProvider();
		}
	}

	private static class PostgreSqlTestFunctionProvider implements TestFunctionProvider {

	}

	private static SqlQuery toSqlQuery(String query) {
		return new SqlQuery() {

			@Override
			public String getSql() {
				return query;
			}

			@Override
			public List<ResultInfo> getResultInfos() {
				return Collections.emptyList();
			}
		};
	}

}
