package com.bakdata.conquery.quarkus.testplugin.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class PluginProviderDirectoryIT {

	@Test
	void exposesPluginInstalledThroughUserProviderDirectory() {
		given()
				.queryParam("format", "json")
				.when().get("/q/openapi")
				.then()
				.statusCode(200)
				.body(
						"components.schemas.MetadataFilterDefinition.discriminator.mapping.PLUGIN_PREFIX",
						equalTo("#/components/schemas/MetadataPluginPrefixFilter")
				)
				.body(
						"components.schemas.MetadataPluginPrefixFilter.properties.type.const",
						equalTo("PLUGIN_PREFIX")
				);
	}
}
