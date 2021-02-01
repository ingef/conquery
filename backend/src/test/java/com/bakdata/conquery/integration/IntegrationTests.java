package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.io.Cloner;
import com.bakdata.conquery.util.support.ConfigOverride;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.classgraph.Resource;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
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
	private static final ObjectWriter CONFIG_WRITER = Jackson.MAPPER.writerFor(ConqueryConfig.class);
	
	private final String defaultTestRoot;
	private final String defaultTestRootPackage;
	@Getter
	private final File workDir;
	@Getter @RegisterExtension
	public static TestConqueryConfig DEFAULT_CONFIG = new TestConqueryConfig();
	private final String defaultConfigString;

	@SneakyThrows(IOException.class)
	public IntegrationTests(String defaultTestRoot, String defaultTestRootPackage) {
		this.defaultTestRoot = defaultTestRoot;
		this.defaultTestRootPackage = defaultTestRootPackage;
		this.workDir = Files.createTempDirectory("conqueryIntegrationTest").toFile();
		TestConquery.configurePortsAndPaths(DEFAULT_CONFIG, this.workDir);
		defaultConfigString = CONFIG_WRITER.writeValueAsString(DEFAULT_CONFIG);
	}

	public List<DynamicNode> jsonTests() {
		final String testRoot = Objects.requireNonNullElse(System.getenv(TestTags.TEST_DIRECTORY_ENVIRONMENT_VARIABLE), defaultTestRoot);

		ResourceTree tree = new ResourceTree(null, null);
		tree.addAll(
			CPSTypeIdResolver.SCAN_RESULT
				.getResourcesMatchingPattern(Pattern.compile("^" + testRoot + ".*\\.test\\.json$"))
		);
		
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
		List<Class<?>> programmatic = CPSTypeIdResolver
			.SCAN_RESULT
			.getClassesImplementing(ProgrammaticIntegrationTest.class.getName())
			.filter(info -> info.getPackageName().startsWith(defaultTestRootPackage))
			.loadClasses();

		TestConquery conquery = new TestConquery(getWorkDir(), getDEFAULT_CONFIG());
		conquery.beforeAll();

		return programmatic
			.stream()
			.<ProgrammaticIntegrationTest>map(c-> {
				try {
					return c.asSubclass(ProgrammaticIntegrationTest.class).getDeclaredConstructor().newInstance();
				}
				catch(Exception e) {
					throw new RuntimeException(e);
				}
			})
			.map(c->
				DynamicTest.dynamicTest(
					c.getClass().getSimpleName(),
					//classpath URI
					URI.create("classpath:/"+c.getClass().getName().replace('.', '/')+".java"),
					new IntegrationTest.Wrapper(c.getClass().getSimpleName(), conquery, c)
				)
			);
	}

	private DynamicNode collectTests(ResourceTree currentDir) {

		if(currentDir.getValue() != null) {
			return readTest(currentDir.getValue(), currentDir.getName(), this);
		}

		List<DynamicNode> list = new ArrayList<>();

		for(ResourceTree child : currentDir.getChildren().values()) {
			list.add(collectTests(child));
		}

		list.sort(Comparator.comparing(DynamicNode::getDisplayName));
		
		return dynamicContainer(
			currentDir.getName(),
			URI.create("classpath:/"+currentDir.getFullName()+"/"),
			list.stream()
		);
	}

	private static final Int2ObjectArrayMap<TestConquery> reusedInstances = new Int2ObjectArrayMap<>();

	private static DynamicTest readTest(Resource resource, String name, IntegrationTests integrationTests) {
		try(InputStream in = resource.open()) {
			JsonIntegrationTest test = new JsonIntegrationTest(in);
			ConqueryConfig conf = Cloner.clone(DEFAULT_CONFIG, Map.of());
			test.overrideConfig(conf);

			name = test.getTestSpec().getLabel();

			int confStringHash = CONFIG_WRITER.writeValueAsString(conf).hashCode();
			if(!reusedInstances.containsKey(confStringHash)){
				log.info("Creating a new test conquery instance for test {}", name);
				TestConquery conquery = new TestConquery(integrationTests.getWorkDir(), conf);
				conquery.beforeAll();
				reusedInstances.put(confStringHash, conquery);
			}
			
			return DynamicTest.dynamicTest(
				name,
				URI.create("classpath:/"+resource.getPath()),
				new IntegrationTest.Wrapper(
					name,
						reusedInstances.get(confStringHash),
						test)
			);
		}
		catch(Exception e) {
			return DynamicTest.dynamicTest(
				name,
				resource.getURI(),
				() -> {
					throw e;
				}
			);
		}
	}

	@EqualsAndHashCode(callSuper = true)
	public static class TestConqueryConfig extends ConqueryConfig  implements Extension, BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) throws Exception {

			context
					.getTestInstance()
					.filter(ConfigOverride.class::isInstance)
					.map(ConfigOverride.class::cast)
					.ifPresent(co -> co.override(this));
		}
	}
}
