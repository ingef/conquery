package com.bakdata.conquery.integration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestFactory;

import com.bakdata.conquery.TestTags;

public class ConqueryIntegrationTests extends IntegrationTests {

	public ConqueryIntegrationTests() {
		super("tests/");
	}
	
	@Override
	@TestFactory @Tag(TestTags.INTEGRATION_JSON)
	public List<DynamicNode> jsonTests() throws IOException {
		return super.jsonTests();
	}
	
	@Override
	@TestFactory @Tag(TestTags.INTEGRATION_PROGRAMMATIC)
	public Stream<DynamicNode> programmaticTests() throws IOException {
		return super.programmaticTests();
	}
}
