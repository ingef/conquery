package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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
				.body("imdb.active", nullValue())
				.body("imdb.detailsAvailable", equalTo(true))
				.body("imdb.tables.size()", equalTo(1))
				.body("imdb.tables[0].id", equalTo("imdb.title"))
				.body("imdb.tables[0].default", equalTo(true))
				.body("imdb.tables[0].selects[0].id", equalTo("imdb.titles.Title"))
				.body("imdb.tables[0].selects[0].label", equalTo("Title"))
				.body("imdb.tables[0].selects[0].default", equalTo(true))
				.body("imdb.tables[0].selects[0].resultType.type", equalTo("STRING"));
	}

	@Test
	void conceptEndpointReturns404ForUnknownConcept() {
		given()
				.when().get("/api/concepts/unknown")
				.then()
				.statusCode(404);
	}
}
