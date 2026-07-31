package com.bakdata.conquery.quarkus.concepts.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.definitions.CountFilterDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FilterDefinitionDeserializationTest {

	@Inject
	ObjectMapper objectMapper;

	@Test
	void deserializesKnownTypeToRegisteredModel() throws Exception {
		FilterDefinition definition = objectMapper.readValue("""
				{"type":"COUNT","name":"events","column":"event_id","distinctByColumn":"person_id","legacyProperty":true}
				""", FilterDefinition.class);

		CountFilterDefinition count = assertInstanceOf(CountFilterDefinition.class, definition);
		assertEquals("events", count.getName());
		assertEquals("event_id", count.getColumn());
		assertEquals(List.of("person_id"), count.getDistinctByColumn());
	}

	@Test
	void keepsUnknownTypeAsExplicitFallbackModel() throws Exception {
		FilterDefinition definition = objectMapper.readValue("""
				{"type":"EXTENSION_FILTER","column":"event_id"}
				""", FilterDefinition.class);

		UnknownFilterDefinition unknown = assertInstanceOf(UnknownFilterDefinition.class, definition);
		assertEquals("EXTENSION_FILTER", unknown.getType());
		assertEquals("event_id", unknown.properties().get("column"));
	}

	@Test
	void acceptsSingleFilterWhereListIsExpected() throws Exception {
		FilterList payload = objectMapper.readValue("""
				{"filters":{"type":"NUMBER","column":"amount"}}
				""", FilterList.class);

		assertEquals(1, payload.filters().size());
		assertEquals("NUMBER", payload.filters().getFirst().getType());
	}

	private record FilterList(List<FilterDefinition> filters) {
	}
}
