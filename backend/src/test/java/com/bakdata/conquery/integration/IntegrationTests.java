package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.integration.json.WorkerTestDataImporter;
import com.bakdata.conquery.integration.sql.dialect.TestSqlConnectorConfig;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.config.DatabaseConfig;
import com.bakdata.conquery.models.config.Dialect;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.config.XodusStoreFactory;
import com.bakdata.conquery.util.support.ConfigOverride;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import io.github.classgraph.Resource;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;

@Slf4j
public class IntegrationTests {

	public static final ObjectMapper MAPPER;
	public static final String JSON_TEST_PATTERN = ".*\\.test\\.json$";
	public static final String SQL_TEST_PATTERN = ".*\\.json$";
	private static final ObjectWriter CONFIG_WRITER;

	static {

		final ObjectMapper mapper = Jackson.MAPPER.copy();

		MAPPER = mapper.setConfig(mapper.getDeserializationConfig().withView(View.Persistence.class))
					   .setConfig(mapper.getSerializationConfig().withView(View.Persistence.class))
					   // to always deserialize into TestSqlConnectorConfig for our tests
					   .addMixIn(SqlConnectorConfig.class, TestSqlConnectorConfig.class);

		CONFIG_WRITER = MAPPER.writerFor(ConqueryConfig.class);
	}

	@Getter
	public final ConqueryConfig config  = new ConqueryConfig();
	private final Map<String, TestConquery> reusedInstances = new HashMap<>();
	private final String defaultTestRoot;
	private final String defaultTestRootPackage;
	@Getter
	private final File workDir;

	@SneakyThrows(IOException.class)
	public IntegrationTests(String defaultTestRoot, String defaultTestRootPackage) {
		this.defaultTestRoot = defaultTestRoot;
		this.defaultTestRootPackage = defaultTestRootPackage;
		this.workDir = Files.createTempDirectory("conqueryIntegrationTest").toFile();
		ConfigOverride.configurePathsAndLogging(this.config, this.workDir);
	}

	private static DynamicContainer toDynamicContainer(ResourceTree currentDir, List<DynamicNode> list) {
		list.sort(Comparator.comparing(DynamicNode::getDisplayName));
		return dynamicContainer(
				currentDir.getName(),
				URI.create("classpath:/" + currentDir.getFullName() + "/"),
				list.stream()
		);
	}

	private static DynamicTest wrapError(Resource resource, String name, Exception e) {
		return DynamicTest.dynamicTest(
				name,
				resource.getURI(),
				() -> {
					throw e;
				}
		);
	}

	private static ResourceTree scanForResources(String testRoot, String pattern) {
		ResourceTree tree = new ResourceTree(null, null);
		tree.addAll(CPSTypeIdResolver.SCAN_RESULT.getResourcesMatchingPattern(Pattern.compile("^" + testRoot + pattern)));
		return tree;
	}

	public List<DynamicNode> jsonTests() {
		TestDataImporter testImporter = new WorkerTestDataImporter();
		final String testRoot = Objects.requireNonNullElse(System.getenv(TestTags.TEST_DIRECTORY_ENVIRONMENT_VARIABLE), defaultTestRoot);
		ResourceTree tree = scanForResources(testRoot, JSON_TEST_PATTERN);
		Dialect dialect = null;
		return collectTestTree(tree, testRoot, testImporter, dialect);
	}

	@SneakyThrows
	public Stream<DynamicNode> sqlProgrammaticTests(DatabaseConfig databaseConfig, TestSqlConnectorConfig sqlConfig, TestDataImporter testDataImporter) {
		this.config.setSqlConnectorConfig(sqlConfig);
		return programmaticTests(testDataImporter, StandaloneSupport.Mode.SQL);
	}

	@SneakyThrows
	public Stream<DynamicNode> programmaticTests(TestDataImporter testImporter, StandaloneSupport.Mode mode) {
		String regexFilter = System.getenv(TestTags.TEST_PROGRAMMATIC_REGEX_FILTER);
		List<Class<?>> programmatic =
				CPSTypeIdResolver.SCAN_RESULT.getClassesImplementing(ProgrammaticIntegrationTest.class.getName())
											 .filter(info -> info.getPackageName().startsWith(defaultTestRootPackage))
											 .filter(classInfo -> {
												 // e.g. For the RestartTest: CONQUERY_TEST_PROGRAMMATIC_REGEX_FILTER=Restart.*

												 if (Strings.isNullOrEmpty(regexFilter)) {
													 // No filter set: allow all tests
													 return true;
												 }
												 return classInfo.getSimpleName().matches(regexFilter);

											 })
											 .loadClasses();

		if (programmatic.isEmpty() && !Strings.isNullOrEmpty(regexFilter)) {
			throw new RuntimeException("No test cases where found using the filter: " + regexFilter);
		}

		return programmatic
				.stream()
				.<ProgrammaticIntegrationTest>map(c -> {
					try {
						return c.asSubclass(ProgrammaticIntegrationTest.class).getDeclaredConstructor().newInstance();
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				})
				.filter(test -> test.isEnabled(mode))
				.map(programmaticIntegrationTest -> createDynamicProgrammaticTestNode(programmaticIntegrationTest, testImporter));
	}

	private DynamicTest createDynamicProgrammaticTestNode(ProgrammaticIntegrationTest test, TestDataImporter testImporter) {
		return DynamicTest.dynamicTest(
				test.getClass().getSimpleName(),
				//classpath URI
				URI.create("classpath:/" + test.getClass().getName().replace('.', '/') + ".java"),
				new IntegrationTest.Wrapper(test.getClass().getSimpleName(), this, test, testImporter)
		);
	}

	@SneakyThrows
	public List<DynamicNode> sqlQueryTests(DatabaseConfig databaseConfig, TestSqlConnectorConfig sqlConfig, TestDataImporter testDataImporter) {
		this.config.setSqlConnectorConfig(sqlConfig);
		final String testRoot = Objects.requireNonNullElse(System.getenv(TestTags.SQL_BACKEND_TEST_DIRECTORY_ENVIRONMENT_VARIABLE), defaultTestRoot);
		ResourceTree tree = scanForResources(testRoot, SQL_TEST_PATTERN);
		return collectTestTree(tree, testRoot, testDataImporter, databaseConfig.getDialect());
	}

	private List<DynamicNode> collectTestTree(ResourceTree tree, String testRoot, TestDataImporter testImporter, Dialect sqlDialect) {
		if (tree.getChildren().isEmpty()) {
			throw new RuntimeException("Could not find tests in " + testRoot);
		}
		final ResourceTree reduced = tree.reduce();

		if (reduced.getChildren().isEmpty()) {
			return Collections.singletonList(collectTests(reduced, testImporter, sqlDialect));
		}
		return reduced.getChildren().values().stream()
					  .map(currentDir -> collectTests(currentDir, testImporter, sqlDialect))
					  .collect(Collectors.toList());
	}

	private DynamicNode collectTests(ResourceTree currentDir, TestDataImporter testImporter, Dialect sqlDialect) {
		if (currentDir.getValue() != null) {
			Optional<DynamicTest> dynamicTest = readTest(currentDir.getValue(), currentDir.getName(), testImporter, sqlDialect);
			if (dynamicTest.isPresent()) {
				return dynamicTest.get();
			}
		}
		List<DynamicNode> list = new ArrayList<>();
		for (ResourceTree child : currentDir.getChildren().values()) {
			list.add(collectTests(child, testImporter, sqlDialect));
		}
		return toDynamicContainer(currentDir, list);
	}

	private Optional<DynamicTest> readTest(Resource resource, String name, TestDataImporter testImporter, Dialect sqlDialect) {
		try (InputStream in = resource.open()) {
			JsonIntegrationTest test = new JsonIntegrationTest(in);
			if (test.getTestSpec().isEnabled(sqlDialect)) {
				return Optional.of(wrapTest(resource, name, test, testImporter));
			}
			return Optional.empty();
		}
		catch (Exception e) {
			return Optional.of(wrapError(resource, name, e));
		}
		finally {
			resource.close();
		}
	}

	private DynamicTest wrapTest(Resource resource, String name, JsonIntegrationTest test, TestDataImporter testImporter) {
		String testLabel = Optional.ofNullable(test.getTestSpec().getLabel())
								   // If no label was defined use the filename part before the first dot
								   .orElse(name.split("\\.", 1)[0]);

		// For easier modification we link the source- not the target-resource of the json tests.
		// Otherwise, a modification might affect the current test runs,
		// but it won't persist over rebuilds or in version control
		final URI compileTargetURI = resource.getURI();
		final URI sourceResourceURI = URI.create(compileTargetURI.toString().replace("/target/test-classes/", "/src/test/resources/"));

		return DynamicTest.dynamicTest(
				testLabel,
				sourceResourceURI,
				new IntegrationTest.Wrapper(
						testLabel,
						this,
						test,
						testImporter
				)
		);
	}

	@SneakyThrows
	public synchronized TestConquery getCachedConqueryInstance(File workDir, ConqueryConfig conf, TestDataImporter testDataImporter) {
		// This should be fast enough and a stable comparison
		String confString = CONFIG_WRITER.writeValueAsString(conf);
		if (!reusedInstances.containsKey(confString)) {

			// For the overriden config we must override the ports and storage path (xodus) so there are no clashes
			// We do it here so the config "hash" is not influenced by the port settings
			ConfigOverride.configureRandomPorts(conf);

			if (conf.getStorage() instanceof XodusStoreFactory storeFactory) {
				ConfigOverride.configureWorkdir(storeFactory, workDir.toPath().resolve(String.valueOf(confString.hashCode())));
			}

			log.trace("Creating a new test conquery instance for test {}", conf);
			TestConquery conquery = new TestConquery(workDir, conf, testDataImporter);
			reusedInstances.put(confString, conquery);

			// Start the fresh instance
			conquery.beforeAll();
		}
		TestConquery conquery = reusedInstances.get(confString);
		return conquery;
	}

}
