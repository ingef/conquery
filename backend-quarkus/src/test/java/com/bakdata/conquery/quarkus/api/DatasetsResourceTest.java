package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DatasetsResourceTest {

	@Test
	void datasetsEndpointRespondsWithConfiguredDatasets() {
		given()
				.when().get("/api/datasets")
				.then()
				.statusCode(200)
				.body("size()", greaterThanOrEqualTo(1))
				.body("[0].id", equalTo("imdb"))
				.body("[0].label", equalTo("IMDb"));
	}

	@Test
	void entityPreviewEndpointRespondsWithConfiguredDefaults() {
		given()
				.when().get("/api/datasets/imdb/entity-preview")
				.then()
				.statusCode(200)
				.body("all[0].name", equalTo("imdb:entities"))
				.body("all[0].label", equalTo("Entities"))
				.body("default[0].name", equalTo("imdb:entities"))
				.body("default[0].label", equalTo("Entities"))
				.body("searchConcept", nullValue())
				.body("searchFilters.size()", equalTo(0));
	}

	@Test
	void entityPreviewEndpointReturns404ForUnknownDataset() {
		given()
				.when().get("/api/datasets/unknown/entity-preview")
				.then()
				.statusCode(404);
	}
}
