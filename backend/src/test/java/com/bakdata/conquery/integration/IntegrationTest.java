package com.bakdata.conquery.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.powerlibraries.io.In;
import io.dropwizard.jersey.validation.Validators;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
public class IntegrationTest {

	public static final ObjectReader TEST_SPEC_READER = Jackson.MAPPER
																.readerFor(ConqueryTestSpec.class);
	@RegisterExtension
	public static final TestConquery CONQUERY = new TestConquery();
	private static final PathMatcher TEST_SPEC_MATCHER = FileSystems.getDefault()
																	.getPathMatcher("glob:**.test.json");

	public static Stream<Arguments> data() throws IOException {
		reduceLogging();
		Path path = Paths.get("tests/");
		if (path.toFile().isDirectory()) {
			return Files.walk(path)
						.filter(IntegrationTest::isTestSpecFile)
						.flatMap(IntegrationTest::read);
		}
		else {
			log.warn("Could not find test directory {}", path.toAbsolutePath());
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

	protected static Stream<Arguments> read(Path path) {
		File file = path.toFile();
		try {
			String content = In.file(file).withUTF8().readAll();
			return Stream.of(Arguments.of(path.getParent().getFileName().toString(), content));
		} catch (IOException e) {
			throw new RuntimeException("Unable to read testSpec " + path, e);
		}
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("data")
	public void test(String name, String testSpec) throws Exception {
		try(StandaloneSupport conquery = CONQUERY.getSupport()) {
			ConqueryTestSpec test = readTest(conquery.getDataset().getId(), testSpec);
	
			Validator validator = Validators.newValidator();
			ValidatorHelper.failOnError(log, validator.validate(test));
	
	
			test.importRequiredData(conquery);

			conquery.waitUntilWorkDone();
	
			test.executeTest(conquery);
		}
	}

	public static ConqueryTestSpec readTest(DatasetId dataset, String testSpec) throws IOException {
		//replace ${dataset} with real dataset name
		testSpec = StringUtils.replace(
			testSpec,
			"${dataset}",
			dataset.toString()
		);
		return TEST_SPEC_READER.readValue(testSpec);
	}
}
