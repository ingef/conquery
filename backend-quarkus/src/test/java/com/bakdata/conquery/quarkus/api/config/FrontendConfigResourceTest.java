package com.bakdata.conquery.quarkus.api.config;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FrontendConfigResourceTest {

	@Test
	void frontendConfigEndpointResponds() {
		given()
				.when().get("/api/config/frontend")
				.then()
				.statusCode(200)
				.body("currency.unit", notNullValue())
				.body("currency.decimalScale", notNullValue())
				.body("queryUpload.table", equalTo("entities"))
				.body("queryUpload.ids[0].name", equalTo("ID"))
				.body("versions[0].name", equalTo("Backend"))
				.body("observationPeriodStart", notNullValue());
	}
}
