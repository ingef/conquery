package com.bakdata.conquery.integration;

import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.json.TestDataImporter;
import com.bakdata.conquery.integration.json.WorkerTestDataImporter;
import com.bakdata.conquery.util.support.StandaloneSupport;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;

public class ConqueryIntegrationTests extends IntegrationTests {

	public static final String DEFAULT_SQL_TEST_ROOT = "tests/sql/";
	public static final TestDataImporter WORKER_TEST_DATA_IMPORTER = new WorkerTestDataImporter();

	public ConqueryIntegrationTests() {
		super("tests/", "com.bakdata.conquery.integration");
	}

	@Override
	@TestFactory
	@Tag(TestTags.INTEGRATION_JSON)
	public List<DynamicNode> jsonTests() {
		return super.jsonTests();
	}

	@TestFactory
	@Tag(TestTags.INTEGRATION_PROGRAMMATIC)
	public Stream<DynamicNode> runProgrammaticTests() {
		return super.programmaticTests(WORKER_TEST_DATA_IMPORTER, StandaloneSupport.Mode.WORKER);
	}

}
