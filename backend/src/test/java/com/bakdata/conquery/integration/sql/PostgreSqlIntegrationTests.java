package com.bakdata.conquery.integration.sql;

import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.apiv1.query.ConceptQuery;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.error.ConqueryError;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.SqlQuery;
import com.bakdata.conquery.sql.conquery.SqlManagedQuery;
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
	private static final String databaseName = "test";
	private static final String username = "user";
	private static final String password = "pass";
	private static DSLContext dslContext;
	private static SqlConnectorConfig sqlConfig;

	public PostgreSqlIntegrationTests() {
		super("tests/", "com.bakdata.conquery.integration");
	}

	@Container
	private static final PostgreSQLContainer<?> postgresqlContainer = new PostgreSQLContainer<>(postgreSqlImageName)
			.withDatabaseName(databaseName)
			.withUsername(username)
			.withPassword(password);


	@BeforeAll
	static void before() {
		postgresqlContainer.start();
		sqlConfig = SqlConnectorConfig.builder()
									  .dialect(Dialect.POSTGRESQL)
									  .jdbcConnectionUrl(postgresqlContainer.getJdbcUrl())
									  .databaseUsername(username)
									  .databasePassword(password)
									  .withPrettyPrinting(true)
									  .primaryColumn("pid")
									  .build();
		dslContext = DslContextFactory.create(sqlConfig);
	}

	@Test
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public void shouldThrowException() {
		SqlExecutionService executionService = new SqlExecutionService(dslContext);
		SqlManagedQuery validQuery = new SqlManagedQuery(new ConceptQuery(), null, null, null, new SqlQuery("SELECT 1"));
		Assertions.assertThatNoException().isThrownBy(() -> executionService.execute(validQuery));

		// executing an empty query should throw an SQL error
		SqlManagedQuery emptyQuery = new SqlManagedQuery(new ConceptQuery(), null, null, null, new SqlQuery(""));
		Assertions.assertThatThrownBy(() -> executionService.execute(emptyQuery))
				  .isInstanceOf(ConqueryError.SqlError.class)
				  .hasMessageContaining("Something went wrong while querying the database: org.postgresql.util.PSQLException");
	}


	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicTest> sqlBackendTests() {
		return super.sqlTests(new TestPostgreSqlDialect(dslContext), sqlConfig);
	}


}
