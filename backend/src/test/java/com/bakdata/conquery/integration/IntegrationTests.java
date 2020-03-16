package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.classgraph.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.extension.RegisterExtension;

@Slf4j @RequiredArgsConstructor
public class IntegrationTests {

	
	private final String defaultTestRoot;
	private final String defaultTestRootPackage;
	
	@RegisterExtension
	public static final TestConquery CONQUERY = new TestConquery();

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
	
	public Stream<DynamicNode> programmaticTests() {
		List<Class<?>> programmatic = CPSTypeIdResolver
			.SCAN_RESULT
			.getClassesImplementing(ProgrammaticIntegrationTest.class.getName())
			.filter(info -> info.getPackageName().startsWith(defaultTestRootPackage))
			.loadClasses();

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
					new IntegrationTest.Wrapper(c.getClass().getSimpleName(), CONQUERY, c)
				)
			);
	}

	private DynamicNode collectTests(ResourceTree currentDir) {

		if(currentDir.getValue() != null) {
			return readTest(currentDir.getValue(), currentDir.getName());
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

	private static DynamicTest readTest(Resource resource, String name) {
		try(InputStream in = resource.open()) {
			JsonNode node = Jackson.MAPPER.readTree(in);
			
			
			if(node.get("label") != null)
				name = node.get("label").asText();
			
			
			
			return DynamicTest.dynamicTest(
				name,
				URI.create("classpath:/"+resource.getPath()),
				new IntegrationTest.Wrapper(
					name,
					CONQUERY,
					new JsonIntegrationTest(node))
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
}
