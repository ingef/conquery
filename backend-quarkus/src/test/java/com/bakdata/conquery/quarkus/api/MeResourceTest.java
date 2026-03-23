package com.bakdata.conquery.quarkus.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

@QuarkusTest
class MeResourceTest {

	@Test
	void meEndpointRespondsWithExpectedShape() {
		given()
				.when().get("/api/me")
				.then()
				.statusCode(200)
				.body("userName", equalTo("anonymous"))
				.body("hideLogoutButton", equalTo(true))
				.body("datasetAbilities", notNullValue())
				.body("groups", notNullValue());
	}

	@Test
	@TestSecurity(user = "testUser")
	void authneticatedMeEndpointRespondsWithExpectedShape() {
		given()
				.when().get("/api/me")
				.then()
				.statusCode(200)
				.body("userName", equalTo("testUser"))
				.body("hideLogoutButton", equalTo(true))
				.body("datasetAbilities", notNullValue())
				.body("groups", notNullValue());
	}
}
