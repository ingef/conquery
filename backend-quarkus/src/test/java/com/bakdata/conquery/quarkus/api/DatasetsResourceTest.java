package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

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
}
