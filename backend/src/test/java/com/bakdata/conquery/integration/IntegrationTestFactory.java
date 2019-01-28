package com.bakdata.conquery.integration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javax.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.jobs.UpdateMatchingStats;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.powerlibraries.io.In;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IntegrationTestFactory {

	public static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER
		.readerFor(ConqueryTestSpec.class);
	private static final Path DEFAULT_TEST_ROOT = Paths.get("tests/");
	
	@RegisterExtension
	public static final TestConquery CONQUERY = new TestConquery();
	private static final PathMatcher TEST_SPEC_MATCHER = FileSystems.getDefault()
		.getPathMatcher("glob:**.test.json");

	@TestFactory
	public static Stream<DynamicContainer> data() throws IOException {
		reduceLogging();
		Path testRoot = DEFAULT_TEST_ROOT;
		//TODO override if system variable is set
		if (testRoot.toFile().isDirectory()) {
			return Files.walk(testRoot)
						.filter(IntegrationTestFactory::isTestSpecFile)
						.sorted()
						.map(IntegrationTestFactory::read);
		}
		else {
			log.warn("Could not find test directory {}", testRoot.toAbsolutePath());
			return Stream.empty();
		}
	}

	public static void reduceLogging() {
		Logger logger = (Logger) LoggerFactory.getLogger("org.hibernate");
		logger.setLevel(Level.WARN);
	}

	protected static boolean isTestSpecFile(Path path) {
		return path.toFile().isFile() && TEST_SPEC_MATCHER.matches(path);
	}

	protected static DynamicContainer readTest(Path testRoot, Path path) {
		File file = path.toFile();
		try {
			String content = In.file(file).withUTF8().readAll();
			
			String name = testRoot.relativize(path.getParent()).toString();
			
			DynamicContainer.dynamicContainer()
			return Arguments.of(
				,
				content
			);
		}
		catch (IOException e) {
			throw new RuntimeException("Unable to read testSpec " + path, e);
		}
	}
}
