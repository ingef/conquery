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
				.body("imdb.parent", nullValue())
				.body("imdb.detailsAvailable", nullValue())
				.body("imdb.codeListResolvable", equalTo(false))
				.body("imdb.additionalInfos[0].key", equalTo("Source"))
				.body("imdb.additionalInfos[0].value", equalTo("IMDb test metadata"))
				.body("imdb.excludeFromTimeAggregation", equalTo(false))
				.body("imdb.tables.size()", equalTo(1))
				.body("imdb.tables[0].id", equalTo("imdb.title"))
				.body("imdb.tables[0].default", equalTo(true))
				.body("imdb.tables[0].dateColumn.tooltip", equalTo("Choose the release date"))
				.body("imdb.tables[0].dateColumn.defaultValue", equalTo("imdb.titles.Release_date"))
				.body("imdb.tables[0].dateColumn.value", nullValue())
				.body("imdb.tables[0].dateColumn.options[0].value", equalTo("imdb.titles.Release_date"))
				.body("imdb.tables[0].dateColumn.options[0].label", equalTo("Release date"))
				.body("imdb.tables[0].selects[0].id", equalTo("imdb.titles.Title"))
				.body("imdb.tables[0].selects[0].label", equalTo("Title"))
				.body("imdb.tables[0].selects[0].default", equalTo(true))
				.body("imdb.tables[0].selects[0].resultType.type", equalTo("STRING"))
				.body("'imdb.movie'.parent", equalTo("imdb"))
				.body("'imdb.movie'.active", nullValue())
				.body("'imdb.movie'.detailsAvailable", nullValue())
				.body("'imdb.movie'.codeListResolvable", equalTo(false))
				.body("'imdb.movie'.additionalInfos[0].key", equalTo("Kind"))
				.body("'imdb.movie'.excludeFromTimeAggregation", nullValue());
	}

	@Test
	void conceptEndpointReturns404ForUnknownConcept() {
		given()
				.when().get("/api/concepts/unknown")
				.then()
				.statusCode(404);
	}
}
