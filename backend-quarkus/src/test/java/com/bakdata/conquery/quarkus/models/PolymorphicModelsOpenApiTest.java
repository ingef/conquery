package com.bakdata.conquery.quarkus.models;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PolymorphicModelsOpenApiTest {

	@Inject
	ObjectMapper objectMapper;

	@Test
	void exposesRegisteredFilterModelsAsDiscriminatedSchema() throws Exception {
		String document = given()
				.queryParam("format", "json")
				.when().get("/q/openapi")
				.then().statusCode(200)
				.extract().asString();
		JsonNode schemas = objectMapper.readTree(document).path("components").path("schemas");
		JsonNode filterSchema = schemas.path("MetadataFilterDefinition");

		assertEquals("type", filterSchema.path("discriminator").path("propertyName").asText());
		assertEquals("#/components/schemas/MetadataCountFilter", filterSchema.path("discriminator").path("mapping").path("COUNT").asText());
		assertTrue(filterSchema.path("oneOf").isArray());
		assertEquals(13, filterSchema.path("oneOf").size());

		JsonNode countSchema = schemas.path("MetadataCountFilter");
		assertTrue(countSchema.isObject());
		assertEquals("COUNT", findProperty(countSchema, "type").path("const").asText());
		assertNotNull(findProperty(countSchema, "column"));
		assertNotNull(findProperty(countSchema, "distinctByColumn"));

		JsonNode selectSchema = schemas.path("MetadataSelectDefinition");
		assertEquals("type", selectSchema.path("discriminator").path("propertyName").asText());
		assertEquals("#/components/schemas/MetadataCountSelect", selectSchema.path("discriminator").path("mapping").path("COUNT").asText());
		assertEquals(13, selectSchema.path("oneOf").size());

		JsonNode countSelectSchema = schemas.path("MetadataCountSelect");
		assertEquals("COUNT", findProperty(countSelectSchema, "type").path("const").asText());
		assertNotNull(findProperty(countSelectSchema, "column"));
		assertNotNull(findProperty(countSelectSchema, "distinctByColumn"));

		JsonNode conceptSelectSchema = schemas.path("MetadataConceptSelectDefinition");
		assertEquals("type", conceptSelectSchema.path("discriminator").path("propertyName").asText());
		assertEquals("#/components/schemas/MetadataExistsConceptSelect", conceptSelectSchema.path("discriminator").path("mapping").path("EXISTS").asText());
		assertEquals(5, conceptSelectSchema.path("oneOf").size());

		JsonNode quarterConceptSelectSchema = schemas.path("MetadataQuarterConceptSelect");
		assertEquals("QUARTER", findProperty(quarterConceptSelectSchema, "type").path("const").asText());
		assertNotNull(findProperty(quarterConceptSelectSchema, "sample"));

		JsonNode conditionSchema = schemas.path("MetadataConceptCondition");
		assertEquals("type", conditionSchema.path("discriminator").path("propertyName").asText());
		assertEquals("#/components/schemas/MetadataEqualConceptCondition", conditionSchema.path("discriminator").path("mapping").path("EQUAL").asText());
		assertEquals("#/components/schemas/MetadataAndConceptCondition", conditionSchema.path("discriminator").path("mapping").path("AND").asText());
		assertEquals(8, conditionSchema.path("oneOf").size());

		JsonNode andConditionSchema = schemas.path("MetadataAndConceptCondition");
		assertEquals("AND", findProperty(andConditionSchema, "type").path("const").asText());
		assertNotNull(findProperty(andConditionSchema, "conditions"));
	}

	private JsonNode findProperty(JsonNode schema, String name) {
		JsonNode property = schema.path("properties").path(name);
		if (!property.isMissingNode()) {
			return property;
		}
		for (JsonNode parent : schema.path("allOf")) {
			JsonNode inherited = findProperty(parent, name);
			if (inherited != null) {
				return inherited;
			}
		}
		return null;
	}
}
