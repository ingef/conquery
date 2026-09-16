package com.bakdata.conquery.quarkus.concepts.conditions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.bakdata.conquery.quarkus.concepts.conditions.definitions.AndConceptCondition;
import com.bakdata.conquery.quarkus.concepts.conditions.definitions.EqualConceptCondition;
import com.bakdata.conquery.quarkus.concepts.conditions.definitions.NotConceptCondition;
import com.bakdata.conquery.quarkus.concepts.conditions.definitions.PresentConceptCondition;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConceptConditionDeserializationTest {

	@Inject
	ObjectMapper objectMapper;

	@Test
	void deserializesNestedRegisteredConditions() throws Exception {
		ConceptCondition condition = objectMapper.readValue("""
				{
				  "type":"AND",
				  "conditions":[
				    {"type":"EQUAL","values":["A00","A01"]},
				    {"type":"NOT","condition":{"type":"PRESENT","column":"deleted_at"}}
				  ]
				}
				""", ConceptCondition.class);

		AndConceptCondition and = assertInstanceOf(AndConceptCondition.class, condition);
		EqualConceptCondition equal = assertInstanceOf(EqualConceptCondition.class, and.getConditions().getFirst());
		assertEquals("EQUAL", equal.getType());
		assertEquals(java.util.List.of("A00", "A01"), equal.getValues());
		NotConceptCondition not = assertInstanceOf(NotConceptCondition.class, and.getConditions().get(1));
		PresentConceptCondition present = assertInstanceOf(PresentConceptCondition.class, not.getCondition());
		assertEquals("deleted_at", present.getColumn());
	}

	@Test
	void keepsUnknownTypeAsExplicitFallbackModel() throws Exception {
		ConceptCondition condition = objectMapper.readValue("""
				{"type":"EXTERNAL_CONDITION","expression":"value != null"}
				""", ConceptCondition.class);

		UnknownConceptCondition unknown = assertInstanceOf(UnknownConceptCondition.class, condition);
		assertEquals("EXTERNAL_CONDITION", unknown.getType());
		assertEquals("value != null", unknown.properties().get("expression"));
	}
}
