package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.support.ConfigOverride;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Strings;
import io.github.classgraph.Resource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

@Slf4j
public class IntegrationTests {
	public static final ObjectMapper MAPPER;
	private static final ObjectWriter CONFIG_WRITER;

	static {
		final ObjectMapper mapper = Jackson.MAPPER.copy();

		MAPPER = mapper.setConfig(mapper.getDeserializationConfig().withView(View.Persistence.class))
					   .setConfig(mapper.getSerializationConfig().withView(View.Persistence.class));

		CONFIG_WRITER = MAPPER.writerFor(ConqueryConfig.class);
	}


	private final Map<String, TestConquery> reusedInstances = new HashMap<>();

	private final String defaultTestRoot;
	private final String defaultTestRootPackage;
	@Getter
	private final File workDir;
	@Getter
	@RegisterExtension
	public static TestConqueryConfig DEFAULT_CONFIG = new TestConqueryConfig();

	@SneakyThrows(IOException.class)
	public IntegrationTests(String defaultTestRoot, String defaultTestRootPackage) {
		this.defaultTestRoot = defaultTestRoot;
		this.defaultTestRootPackage = defaultTestRootPackage;
		this.workDir = Files.createTempDirectory("conqueryIntegrationTest").toFile();
		TestConquery.configurePathsAndLogging(DEFAULT_CONFIG, this.workDir);
	}

	public List<DynamicNode> jsonTests() {
		final String testRoot = Objects.requireNonNullElse(System.getenv(TestTags.TEST_DIRECTORY_ENVIRONMENT_VARIABLE), defaultTestRoot);

		ResourceTree tree = new ResourceTree(null, null);
		tree.addAll(CPSTypeIdResolver.SCAN_RESULT.getResourcesMatchingPattern(Pattern.compile("^" + testRoot + ".*\\.test\\.json$")));

		// collect tests from directory
		if (tree.getChildren().isEmpty()) {
			log.warn("Could not find tests in {}", testRoot);
			return Collections.emptyList();
		}
		final ResourceTree reduced = tree.reduce();

		if (reduced.getChildren().isEmpty()) {
			return Collections.singletonList(collectTests(reduced));
		}
		return reduced.getChildren().values().stream()
					  .map(this::collectTests)
					  .collect(Collectors.toList());
	}

	@SneakyThrows
	public Stream<DynamicNode> programmaticTests() {
		List<Class<?>> programmatic =
				CPSTypeIdResolver.SCAN_RESULT.getClassesImplementing(ProgrammaticIntegrationTest.class.getName())
											 .filter(info -> info.getPackageName().startsWith(defaultTestRootPackage))
											 .filter(classInfo -> {
												 String regexFilter = System.getenv(TestTags.TEST_PROGRAMMATIC_REGEX_FILTER);
												 if (Strings.isNullOrEmpty(regexFilter)) {
													 // No filter set: allow all tests
													 return true;
												 }
												 return classInfo.getSimpleName().matches(regexFilter);

											 })
											 .loadClasses();

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
				.map(this::createDynamicProgrammaticTestNode);
	}

	private DynamicTest createDynamicProgrammaticTestNode(ProgrammaticIntegrationTest test) {
		return DynamicTest.dynamicTest(
				test.getClass().getSimpleName(),
				//classpath URI
				URI.create("classpath:/" + test.getClass().getName().replace('.', '/') + ".java"),
				new IntegrationTest.Wrapper(test.getClass().getSimpleName(), this, test)
		);
	}

	private DynamicNode collectTests(ResourceTree currentDir) {

		if (currentDir.getValue() != null) {
			return readTest(currentDir.getValue(), currentDir.getName());
		}

		List<DynamicNode> list = new ArrayList<>();

		for (ResourceTree child : currentDir.getChildren().values()) {
			list.add(collectTests(child));
		}

		list.sort(Comparator.comparing(DynamicNode::getDisplayName));

		return dynamicContainer(
				currentDir.getName(),
				URI.create("classpath:/" + currentDir.getFullName() + "/"),
				list.stream()
		);
	}

	private DynamicTest readTest(Resource resource, String name) {
		try (InputStream in = resource.open()) {
			JsonIntegrationTest test = new JsonIntegrationTest(in);

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
							test
					)
			);
		}
		catch (Exception e) {
			return DynamicTest.dynamicTest(
					name,
					resource.getURI(),
					() -> {
						throw e;
					}
			);
		}
	}

	@SneakyThrows
	public synchronized TestConquery getCachedConqueryInstance(File workDir, ConqueryConfig conf) {
		// This should be fast enough and a stable comparison
		String confString = CONFIG_WRITER.writeValueAsString(conf);
		if (!reusedInstances.containsKey(confString)) {
			// For the overriden config we must override the ports so there are no clashes
			// We do it here so the config "hash" is not influenced by the port settings
			TestConquery.configureRandomPorts(conf);
			log.trace("Creating a new test conquery instance for test {}", conf);
			TestConquery conquery = new TestConquery(workDir, conf);
			reusedInstances.put(confString, conquery);
			conquery.beforeAll();
		}
		TestConquery conquery = reusedInstances.get(confString);
		return conquery;
	}

	@EqualsAndHashCode(callSuper = true)
	public static class TestConqueryConfig extends ConqueryConfig implements Extension, BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) throws Exception {

			context.getTestInstance()
				   .filter(ConfigOverride.class::isInstance)
				   .map(ConfigOverride.class::cast)
				   .ifPresent(co -> co.override(this));
		}
	}
}
