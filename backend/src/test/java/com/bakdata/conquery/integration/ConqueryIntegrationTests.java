package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.sql.SqlIntegrationTest;
import com.bakdata.conquery.sql.conversion.SqlConverterService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConqueryIntegrationTests extends IntegrationTests {

	private static final String SQL_TEST_DIR = "src/test/resources/tests/sql";

	public ConqueryIntegrationTests() {
		super("tests/", "com.bakdata.conquery.integration");
	}

	@Override
	@TestFactory
	@Tag(TestTags.INTEGRATION_JSON)
	public List<DynamicNode> jsonTests() {
		return super.jsonTests();
	}

	@Override
	@TestFactory
	@Tag(TestTags.INTEGRATION_PROGRAMMATIC)
	public Stream<DynamicNode> programmaticTests() {
		return super.programmaticTests();
	}

	@ParameterizedTest
	@MethodSource("sqlBackendTestsProvider")
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public void sqlBackendTests(final SqlIntegrationTest test) throws IOException {
		SqlConverterService converter = new SqlConverterService();
		Assertions.assertEquals(test.getExpectedSql(), converter.convert(test.getQuery()));
	}

	public static Stream<Arguments> sqlBackendTestsProvider() throws IOException {
		final Path testRootDir = Path.of(Objects.requireNonNullElse(
				System.getenv(TestTags.SQL_BACKEND_TEST_DIRECTORY_ENVIRONMENT_VARIABLE),
				SQL_TEST_DIR
		));

		Stream<Path> paths = Files.walk(testRootDir);
		return paths.filter(path -> !Files.isDirectory(path) && path.toString().endsWith(".json"))
					.map(SqlIntegrationTest::fromJsonSpec)
					.map(test -> arguments(named(test.getLabel(), test)));
	}
}
