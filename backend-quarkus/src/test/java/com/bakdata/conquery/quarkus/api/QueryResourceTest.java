package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class QueryResourceTest {

	@Test
	void queryEndpointRespondsWithNewStatus() {
		String queryId = createQuery();

		given()
				.when().get("/api/queries/{queryId}", queryId)
				.then()
				.statusCode(200)
				.body("id", equalTo(queryId))
				.body("label", equalTo("Query " + queryId))
				.body("status", equalTo("NEW"))
				.body("query.type", equalTo("CONCEPT_QUERY"))
				.body("createdAt", notNullValue());
	}

	@Test
	void cancelQueryEndpointResponds() {
		String queryId = createQuery();

		given()
				.when().post("/api/queries/{queryId}/cancel", queryId)
				.then()
				.statusCode(204);
	}

	private String createQuery() {
		return given()
				.contentType("application/json")
				.body("{\"type\":\"CONCEPT_QUERY\"}")
				.when().post("/api/datasets/imdb/queries")
				.then()
				.statusCode(200)
				.body("id", notNullValue())
				.extract().path("id");
	}
}
