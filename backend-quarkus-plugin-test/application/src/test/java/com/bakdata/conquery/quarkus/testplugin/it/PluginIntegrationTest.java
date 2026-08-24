package com.bakdata.conquery.quarkus.testplugin.it;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinition;
import com.bakdata.conquery.quarkus.concepts.filters.FilterDefinitionAssembler;
import com.bakdata.conquery.quarkus.ids.ColumnId;
import com.bakdata.conquery.quarkus.ids.ConceptId;
import com.bakdata.conquery.quarkus.ids.ConnectorId;
import com.bakdata.conquery.quarkus.ids.DatasetId;
import com.bakdata.conquery.quarkus.ids.TableId;
import com.bakdata.conquery.quarkus.models.PolymorphicModelRegistry;
import com.bakdata.conquery.quarkus.storage.DatasetCatalogRepository;
import com.bakdata.conquery.quarkus.testplugin.PrefixFilterDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PluginIntegrationTest {

	@Inject
	ObjectMapper objectMapper;

	@Inject
	PolymorphicModelRegistry modelRegistry;

	@Inject
	FilterDefinitionAssembler assembler;

	@Test
	void discoversPluginProviderAndUsesItEndToEnd() throws Exception {
		FilterDefinition definition = objectMapper.readValue("""
				{"type":"PLUGIN_PREFIX","name":"plugin_filter","label":"Plugin filter","column":"code","prefix":"ABC"}
				""", FilterDefinition.class);

		PrefixFilterDefinition pluginDefinition = assertInstanceOf(PrefixFilterDefinition.class, definition);
		assertEquals("ABC", pluginDefinition.getPrefix());
		assertTrue(modelRegistry.find(FilterDefinition.class, "PLUGIN_PREFIX").isPresent());

		DatasetId datasetId = new DatasetId("plugin-test");
		TableId tableId = new TableId(datasetId, "events");
		ConceptId conceptId = new ConceptId(datasetId, List.of("plugin-concept"));
		ConnectorId connectorId = new ConnectorId(conceptId, "plugin-connector");
		ColumnId columnId = new ColumnId(tableId, "code");
		DatasetCatalogRepository.TableRecord table = new DatasetCatalogRepository.TableRecord(
				tableId,
				"Events",
				List.of(new DatasetCatalogRepository.ColumnRecord(columnId, "Code", DatasetCatalogRepository.ColumnType.STRING, null)),
				columnId
		);

		DatasetCatalogRepository.Filter filter = assembler.assemble(
				connectorId,
				tableId,
				table,
				List.of(definition),
				(idType, context, fallbackValue, sanitized) -> { },
				true
		).getFirst();

		assertEquals("plugin_filter", filter.id().name());
		assertEquals("STRING", filter.type());
		assertEquals("ABC.*", filter.pattern());
		assertEquals("ABC", filter.defaultValue());
		assertEquals(List.of(columnId), filter.requiredColumns());
	}

	@Test
	void exposesPluginModelInOpenApi() throws Exception {
		String document = given()
				.queryParam("format", "json")
				.when().get("/q/openapi")
				.then().statusCode(200)
				.extract().asString();
		JsonNode schemas = objectMapper.readTree(document).path("components").path("schemas");

		assertEquals(
				"#/components/schemas/MetadataPluginPrefixFilter",
				schemas.path("MetadataFilterDefinition").path("discriminator").path("mapping").path("PLUGIN_PREFIX").asText()
		);
		JsonNode pluginSchema = schemas.path("MetadataPluginPrefixFilter");
		assertEquals("PLUGIN_PREFIX", pluginSchema.path("properties").path("type").path("const").asText());
		assertTrue(hasProperty(pluginSchema, "column"));
		assertTrue(hasProperty(pluginSchema, "prefix"));
	}

	private boolean hasProperty(JsonNode schema, String name) {
		if (!schema.path("properties").path(name).isMissingNode()) {
			return true;
		}
		for (JsonNode parent : schema.path("allOf")) {
			if (hasProperty(parent, name)) {
				return true;
			}
		}
		return false;
	}
}
