package com.bakdata.conquery.integration.sql.dialect;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.IntegrationTests;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.datasets.concepts.select.Select;
import com.bakdata.conquery.sql.DslContextFactory;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.DateDistanceSelectConverter;
import com.bakdata.conquery.sql.conversion.cqelement.concept.select.SelectConverter;
import com.bakdata.conquery.sql.conversion.dialect.ClickHouseDialect;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RowN;
import org.jooq.Table;
import org.jooq.conf.ParamType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Disabled
@Testcontainers
@Slf4j
public class ClickHouseIntegrationTests extends IntegrationTests {

	private static final DockerImageName CLICK_HOUSE_IMAGE_NAME = DockerImageName.parse("clickhouse/clickhouse-server:23.7-alpine");
	private static final String USERNAME = "default";
	private static final String PASSWORD = "";
	private static final String CONFIG_RESOURCE_NAME = "/sql/clickhouse-config/user.xml";
	private static final String CONFIG_CONTAINER_PATH = "/etc/clickhouse-server/users.d/user.xml";
	@Container
	private static final ClickHouseContainer CLICK_HOUSE_CONTAINER = new ClickHouseContainer(CLICK_HOUSE_IMAGE_NAME);
	private static DSLContext dslContext;
	private static SqlConnectorConfig sqlConfig;

	public ClickHouseIntegrationTests() {
		super("tests/", "com.bakdata.conquery.integration");
	}

	@BeforeAll
	static void before() {
		CLICK_HOUSE_CONTAINER.copyFileToContainer(MountableFile.forClasspathResource(CONFIG_RESOURCE_NAME), CONFIG_CONTAINER_PATH);
		CLICK_HOUSE_CONTAINER.start();

		sqlConfig = SqlConnectorConfig.builder()
									  .dialect(Dialect.CLICKHOUSE)
									  .jdbcConnectionUrl(CLICK_HOUSE_CONTAINER.getJdbcUrl())
									  .databaseUsername(USERNAME)
									  .databasePassword(PASSWORD)
									  .withPrettyPrinting(true)
									  .primaryColumn("pid")
									  .build();
		dslContext = DslContextFactory.create(sqlConfig);
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicTest> sqlBackendTests() {
		return super.sqlTests(new TestClickHouseDialect(dslContext), sqlConfig);
	}

	private static class TestClickHouseDialect extends ClickHouseDialect implements TestSqlDialect {

		public TestClickHouseDialect(DSLContext dslContext) {
			super(dslContext);
		}

		@Override
		public List<SelectConverter<? extends Select>> getSelectConverters() {
			return this.customizeSelectConverters(List.of(
					new DateDistanceSelectConverter(new MockDateNowSupplier())
			));
		}

		@Override
		public TestFunctionProvider getTestFunctionProvider() {
			return new ClickHouseTestFunctionProvider();
		}
	}

	private static class ClickHouseTestFunctionProvider implements TestFunctionProvider {

		@Override
		public String createTableStatement(Table<Record> table, List<Field<?>> columns, DSLContext dslContext) {
			String statement = dslContext.createTable(table)
										 .columns(columns)
										 .getSQL(ParamType.INLINED);

			Pattern pattern = Pattern.compile("date null", Pattern.CASE_INSENSITIVE);
			statement = pattern.matcher(statement).replaceAll("Date32 null");
			statement += String.format(" ENGINE = MergeTree() ORDER BY \"%s\";", sqlConfig.getPrimaryColumn());
			return statement;
		}

		@Override
		public void insertValuesIntoTable(Table<Record> table, List<Field<?>> columns, List<RowN> content, Statement statement, DSLContext dslContext)
				throws SQLException {
			// Convert java.sql.Date fields to String because JOOQ attempts to insert them as Date types, while ClickHouse expects Date32 types
			columns = columns.stream().map(field -> field.getDataType().isDate() ? field.coerce(String.class) : field).toList();
			String insertIntoTableStatement = dslContext.insertInto(table, columns)
														.valuesOfRows(content)
														.getSQL(ParamType.INLINED);
			statement.execute(insertIntoTableStatement);
		}
	}

}
