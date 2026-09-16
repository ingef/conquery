package com.bakdata.conquery.quarkus.concepts.selects.concept;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.selects.concept.definitions.QuarterConceptSelectDefinition;
import com.bakdata.conquery.quarkus.ids.ConceptSelectId;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@QuarkusTest
class ConceptSelectDefinitionDeserializationTest {

	@Inject
	ObjectMapper objectMapper;

	@ParameterizedTest
	@ValueSource(strings = {"EXISTS", "QUARTER", "EVENT_DATE_UNION", "EVENT_DURATION_SUM", "CONCEPT_VALUES"})
	void deserializesEveryRegisteredType(String type) throws Exception {
		String sample = type.equals("QUARTER") ? ",\"sample\":\"EARLIEST\"" : "";
		ConceptSelectDefinition definition = objectMapper.readValue("{\"type\":\"" + type + "\"" + sample + "}", ConceptSelectDefinition.class);

		assertFalse(definition instanceof UnknownConceptSelectDefinition, type);
		assertEquals(type, definition.getType());
	}

	@Test
	void acceptsScalarWhereConceptSelectListIsExpected() throws Exception {
		ConceptSelectList payload = objectMapper.readValue("""
				{"selects":{"type":"QUARTER","sample":"LATEST"}}
				""", ConceptSelectList.class);

		QuarterConceptSelectDefinition quarter = assertInstanceOf(QuarterConceptSelectDefinition.class, payload.selects().getFirst());
		assertEquals(QuarterConceptSelectDefinition.TemporalSampler.LATEST, quarter.getSample());
	}

	@Test
	void keepsUnknownTypeAsExplicitFallbackModel() throws Exception {
		ConceptSelectDefinition definition = objectMapper.readValue("""
				{"type":"EXTENSION_CONCEPT_SELECT","option":true}
				""", ConceptSelectDefinition.class);

		UnknownConceptSelectDefinition unknown = assertInstanceOf(UnknownConceptSelectDefinition.class, definition);
		assertEquals(true, unknown.properties().get("option"));
	}

	@Test
	void serializesTypedConceptSelectIdAsString() throws Exception {
		ConceptSelectId nested = ConceptSelectId.parse("demo.concept.select");
		ConceptSelectId root = ConceptSelectId.parse("demo.select");

		assertEquals("demo.concept.select", nested.toString());
		assertEquals("demo.concept", nested.conceptId().toString());
		assertEquals("demo.select", root.toString());
		assertEquals("demo", root.conceptId().toString());
		assertEquals("\"demo.concept.select\"", objectMapper.writeValueAsString(nested));
		assertEquals(nested, objectMapper.readValue("\"demo.concept.select\"", ConceptSelectId.class));
	}

	private record ConceptSelectList(List<ConceptSelectDefinition> selects) {
	}
}
