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
		given()
				.when().get("/api/queries/example-query")
				.then()
				.statusCode(200)
				.body("id", equalTo("example-query"))
				.body("label", equalTo("Query example-query"))
				.body("status", equalTo("NEW"))
				.body("queryType", equalTo("CONCEPT_QUERY"))
				.body("createdAt", notNullValue());
	}
}
