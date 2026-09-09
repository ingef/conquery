package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PingResourceTest {

	@Test
	void pingEndpointResponds() {
		given()
				.when().get("/api/ping")
				.then()
				.statusCode(200)
				.body("status", equalTo("ok"))
				.body("service", equalTo("conquery-backend-quarkus"));
	}
}
