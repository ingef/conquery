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
