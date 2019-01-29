package com.bakdata.conquery.integration;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationTests {

	
	private static final File DEFAULT_TEST_ROOT = new File("tests/");
	
	@RegisterExtension
	public static final TestConquery CONQUERY = new TestConquery();

	@TestFactory @Tag(TestTags.INTEGRATION_JSON)
	public List<DynamicNode> jsonTests() throws IOException {
		reduceLogging();
		File testRoot = DEFAULT_TEST_ROOT;
		if(System.getenv(TestTags.TEST_DIRECTORY_ENVIRONMENT_VARIABLE) != null)
			testRoot = new File(System.getenv(TestTags.TEST_DIRECTORY_ENVIRONMENT_VARIABLE));

		
		//collect tests from directory
		if (testRoot.isDirectory()) {
			return collectTests(testRoot, testRoot).getChildren().collect(Collectors.toList());
		}
		else {
			log.warn("Could not find test directory {}", testRoot.getAbsolutePath());
			return Collections.emptyList();
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
					URI.create("classpath:/"+c.getClass().getName().replace('.', '/')+".java?line=1"),
					new IntegrationTest.Wrapper(CONQUERY, c)
				)
			);
	}

	private static DynamicContainer collectTests(File testRoot, File currentDir) {
		List<DynamicNode> list = new ArrayList<>();
		
		for(File child : currentDir.listFiles()) {
			if(child.isDirectory()) {
				list.add(collectTests(testRoot, child));
			}
			else if(child.isFile() && isTestSpecFile(child)) {
				list.add(readTest(child));
			}
		}
		
		return dynamicContainer(
			currentDir.getName(),
			currentDir.getAbsoluteFile().toURI(),
			list.stream()
		);
	}

	public static void reduceLogging() {
		Logger logger = (Logger) LoggerFactory.getLogger("org.hibernate");
		logger.setLevel(Level.WARN);
	}

	private static boolean isTestSpecFile(File file) {
		return file.isFile() && file.getName().endsWith(".test.json");
	}

	private static DynamicTest readTest(File testFile) {
		try {
			JsonNode node = Jackson.MAPPER.readTree(testFile);
			String name = testFile.getName();
			if(node.get("label") != null)
				name = node.get("label").asText();
			
			
			
			return DynamicTest.dynamicTest(
				name,
				testFile.getAbsoluteFile().toURI(),
				new IntegrationTest.Wrapper(CONQUERY, new JsonIntegrationTest(node))
			);
		}
		catch(Exception e) {
			return DynamicTest.dynamicTest(
				testFile.getParent(),
				testFile.getAbsoluteFile().toURI(),
				() -> {
					throw e;
				}
			);
		}
	}
}
