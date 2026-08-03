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

	@Test
	void queryEndpointRoundTripsTypedFilterValues() {
		String queryId = given()
				.contentType("application/json")
				.body("""
						{
						  "type":"CONCEPT_QUERY",
						  "root":{
						    "type":"CONCEPT",
						    "tables":[{
						      "id":"imdb.titles",
						      "filters":[{
						        "filter":"imdb.titles.release_age",
						        "type":"INTEGER_RANGE",
						        "value":{"min":1,"max":10}
						      }]
						    }]
						  }
						}
						""")
				.when().post("/api/datasets/imdb/queries")
				.then().statusCode(200)
				.extract().path("id");

		given()
				.when().get("/api/queries/{queryId}", queryId)
				.then().statusCode(200)
				.body("query.root.tables[0].filters[0].type", equalTo("INTEGER_RANGE"))
				.body("query.root.tables[0].filters[0].filter", equalTo("imdb.titles.release_age"))
				.body("query.root.tables[0].filters[0].value.min", equalTo(1))
				.body("query.root.tables[0].filters[0].value.max", equalTo(10));
	}

	@Test
	void queryEndpointRejectsUnknownFilterValueType() {
		given()
				.contentType("application/json")
				.body("""
						{
						  "type":"CONCEPT_QUERY",
						  "root":{
						    "type":"CONCEPT",
						    "tables":[{"filters":[{
						      "filter":"imdb.titles.release_age",
						      "type":"EXTERNAL_VALUE",
						      "value":1
						    }]}]
						  }
						}
						""")
				.when().post("/api/datasets/imdb/queries")
				.then().statusCode(400);
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
