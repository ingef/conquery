package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ConceptResourceTest {

	@Test
	void conceptEndpointReturnsConceptMap() {
		given()
				.when().get("/api/concepts/imdb")
				.then()
				.statusCode(200)
				.body("imdb.label", equalTo("IMDb"))
				.body("imdb.detailsAvailable", equalTo(true))
				.body("imdb.tables.size()", equalTo(0))
				.body("imdb.selects.size()", equalTo(0));
	}

	@Test
	void conceptEndpointReturns404ForUnknownConcept() {
		given()
				.when().get("/api/concepts/unknown")
				.then()
				.statusCode(404);
	}
}
