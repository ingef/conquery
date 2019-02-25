package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.tests.ProgrammaticIntegrationTest;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.JsonNode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.github.classgraph.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationTests {

	
	private static final String DEFAULT_TEST_ROOT = "tests/";
	
	@RegisterExtension
	public static final TestConquery CONQUERY = new TestConquery();

	@TestFactory @Tag(TestTags.INTEGRATION_JSON)
	public List<DynamicNode> jsonTests() throws IOException {
		final String testRoot = Objects.requireNonNullElse(
			System.getenv(TestTags.TEST_DIRECTORY_ENVIRONMENT_VARIABLE),
			DEFAULT_TEST_ROOT
		);
		
		ResourceTree tree = new ResourceTree(null, null);
		tree.addAll(
			CPSTypeIdResolver.SCAN_RESULT
				.getResourcesMatchingPattern(Pattern.compile("^" + testRoot + ".*\\.test\\.json$"))
		);
		
		//collect tests from directory
		if (tree.getChildren().isEmpty()) {
			log.warn("Could not find tests in {}", testRoot);
			return Collections.emptyList();
		}
		else {
			return tree.reduce().getChildren().values()
				.stream()
				.map(this::collectTests)
				.collect(Collectors.toList());
		}
	}
	
	@TestFactory @Tag(TestTags.INTEGRATION_PROGRAMMATIC)
	public Stream<DynamicNode> programmaticTests() throws IOException {
		List<Class<?>> programmatic = CPSTypeIdResolver
			.SCAN_RESULT
			.getClassesImplementing(ProgrammaticIntegrationTest.class.getName())
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
					new IntegrationTest.Wrapper(CONQUERY, c)
				)
			);
	}

	private DynamicContainer collectTests(ResourceTree currentDir) {
		List<DynamicNode> list = new ArrayList<>();
		
		for(ResourceTree child : currentDir.getChildren().values()) {
			if(!child.getChildren().isEmpty()) {
				list.add(collectTests(child));
			}
			else if(child.getValue() != null) {
				list.add(readTest(child.getValue(), child.getName()));
			}
		}
		
		list.sort(Comparator.comparing(DynamicNode::getDisplayName));
		
		return dynamicContainer(
			currentDir.getName(),
			URI.create("classpath:/"+currentDir.getFullName()),
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
				resource.getURL().toURI(),
				new IntegrationTest.Wrapper(CONQUERY, new JsonIntegrationTest(node))
			);
		}
		catch(Exception e) {
			try {
				return DynamicTest.dynamicTest(
					name,
					resource.getURL().toURI(),
					() -> {
						throw e;
					}
				);
			}
			catch (URISyntaxException e1) {
				log.error("Failed while trying to create errored test", e1);
				return DynamicTest.dynamicTest(
					name,
					() -> {
						throw e;
					}
				);
			}
		}
	}
}
