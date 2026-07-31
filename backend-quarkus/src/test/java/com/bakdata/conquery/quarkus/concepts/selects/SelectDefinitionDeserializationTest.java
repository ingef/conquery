package com.bakdata.conquery.quarkus.concepts.selects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.definitions.CountSelectDefinition;
import com.bakdata.conquery.quarkus.ids.SelectId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class SelectDefinitionDeserializationTest {

	@Inject
	ObjectMapper objectMapper;

	@ParameterizedTest
	@ValueSource(strings = {"DISTINCT", "FIRST", "LAST", "RANDOM", "COUNT", "COUNT_QUARTERS", "DATE_DISTANCE", "DATE_UNION", "DURATION_SUM", "FLAGS", "PREFIX", "QUARTERS_IN_YEAR", "SUM"})
	void deserializesEveryBuiltinType(String type) throws Exception {
		SelectDefinition definition = objectMapper.readValue("{\"type\":\"" + type + "\"}", SelectDefinition.class);

		assertFalse(definition instanceof UnknownSelectDefinition, type);
		assertEquals(type, definition.getType());
	}

	@Test
	void acceptsScalarsWhereSelectAndNestedColumnListsAreExpected() throws Exception {
		SelectList payload = objectMapper.readValue("""
				{"selects":{"type":"COUNT","column":"event_id","distinctByColumn":"person_id"}}
				""", SelectList.class);

		CountSelectDefinition count = assertInstanceOf(CountSelectDefinition.class, payload.selects().getFirst());
		assertEquals(List.of("person_id"), count.getDistinctByColumn());
	}

	@Test
	void keepsUnknownTypeAsExplicitFallbackModel() throws Exception {
		SelectDefinition definition = objectMapper.readValue("""
				{"type":"EXTENSION_SELECT","column":"event_id"}
				""", SelectDefinition.class);

		UnknownSelectDefinition unknown = assertInstanceOf(UnknownSelectDefinition.class, definition);
		assertEquals("event_id", unknown.properties().get("column"));
	}

	@Test
	void serializesTypedSelectIdAsString() throws Exception {
		SelectId id = SelectId.parse("demo.concept.connector.select");

		assertEquals("demo.concept.connector.select", id.toString());
		assertEquals("\"demo.concept.connector.select\"", objectMapper.writeValueAsString(id));
		assertEquals(id, objectMapper.readValue("\"demo.concept.connector.select\"", SelectId.class));
		assertEquals("demo", id.datasetId().toString());
	}

	private record SelectList(List<SelectDefinition> selects) {
	}
}
