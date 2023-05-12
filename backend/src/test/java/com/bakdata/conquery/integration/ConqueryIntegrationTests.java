package com.bakdata.conquery.integration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.bakdata.conquery.TestTags;
import com.bakdata.conquery.integration.sql.SqlIntegrationTest;
import com.bakdata.conquery.models.exceptions.JSONException;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ConqueryIntegrationTests extends IntegrationTests {


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
	@TestFactory
	@Tag(TestTags.INTEGRATION_SQL_BACKEND)
	public Stream<DynamicTest> sqlBackendTests() {
		return super.sqlTests();
	}

}
