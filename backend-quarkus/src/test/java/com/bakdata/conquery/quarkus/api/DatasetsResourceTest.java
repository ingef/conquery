package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
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

	@Test
	void conceptsEndpointRespondsWithFrontendCompatibleShape() {
		given()
				.when().get("/api/datasets/imdb/concepts")
				.then()
				.statusCode(200)
				.body("secondaryIds.size()", equalTo(0))
				.body("concepts.imdb.label", equalTo("IMDb"))
				.body("concepts.imdb.detailsAvailable", equalTo(true))
				.body("concepts.imdb.children.size()", equalTo(0));
	}

	@Test
	void conceptsEndpointReturns404ForUnknownDataset() {
		given()
				.when().get("/api/datasets/unknown/concepts")
				.then()
				.statusCode(404);
	}

	@Test
	void formQueriesEndpointReturnsForms() {
		given()
				.when().get("/api/datasets/imdb/form-queries")
				.then()
				.statusCode(200)
				.body("size()", greaterThanOrEqualTo(1))
				.body("[0].type", equalTo("EXPORT_FORM"))
				.body("[0].fields.size()", greaterThanOrEqualTo(1));
	}

	@Test
	void formQueriesEndpointReturns404ForUnknownDataset() {
		given()
				.when().get("/api/datasets/unknown/form-queries")
				.then()
				.statusCode(404);
	}

	@Test
	void queriesEndpointReturnsEmptyList() {
		given()
				.when().get("/api/datasets/imdb/queries")
				.then()
				.statusCode(200)
				.body("size()", equalTo(0));
	}

	@Test
	void queriesEndpointReturns404ForUnknownDataset() {
		given()
				.when().get("/api/datasets/unknown/queries")
				.then()
				.statusCode(404);
	}

	@Test
	void postQueriesEndpointReturnsCreatedQueryId() {
		given()
				.contentType("application/json")
				.body("{\"type\":\"CONCEPT_QUERY\"}")
				.when().post("/api/datasets/imdb/queries")
				.then()
				.statusCode(200)
				.body("id", notNullValue());
	}

	@Test
	void postQueriesEndpointReturns404ForUnknownDataset() {
		given()
				.contentType("application/json")
				.body("{\"type\":\"CONCEPT_QUERY\"}")
				.when().post("/api/datasets/unknown/queries")
				.then()
				.statusCode(404);
	}
}
