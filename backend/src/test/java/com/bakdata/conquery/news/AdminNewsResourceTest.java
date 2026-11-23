package com.bakdata.conquery.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.bakdata.conquery.io.jetty.IllegalArgumentExceptionMapper;
import com.bakdata.conquery.io.jetty.JsonValidationExceptionMapper;
import com.bakdata.conquery.io.jetty.NoSuchElementExceptionMapper;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
class AdminNewsResourceTest {

	private static final NewsItem NEWS_ITEM = new NewsItem(
			"release-1",
			"Release 1",
			"The first release",
			URI.create("https://example.com/releases/1"),
			LocalDate.of(2026, 9, 30),
			Set.of("dataset-a")
	);
	private static final InMemoryNewsService NEWS_SERVICE = new InMemoryNewsService();
	private static final ResourceExtension EXT = ResourceExtension.builder()
			.addResource(new AdminNewsResource(NEWS_SERVICE))
			.setRegisterDefaultExceptionMappers(false)
			.addProvider(JsonValidationExceptionMapper.class)
			.addProvider(NoSuchElementExceptionMapper.class)
			.addProvider(IllegalArgumentExceptionMapper.class)
			.build();

	@BeforeEach
	void clearNews() {
		NEWS_SERVICE.clear();
	}

	@Test
	void listsAllNewsItems() throws IOException {
		NEWS_SERVICE.addNewsItem(NEWS_ITEM);

		try(Response response = EXT.target("/news").request().get()) {
			assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
			JsonNode body = response.readEntity(JsonNode.class);
			assertThat(body).hasSize(1);
			assertThat(body.get(0).get("id").asText()).isEqualTo("release-1");
			assertThat(body.get(0).get("categories").get(0).asText()).isEqualTo("dataset-a");
		}
	}

	@Test
	void addsNewsItem() {
		try(Response response = EXT.target("/news").request()
				.post(Entity.entity(newsJson(NEWS_ITEM), MediaType.APPLICATION_JSON_TYPE))) {
			assertThat(response.getStatus()).isEqualTo(Response.Status.CREATED.getStatusCode());
			assertThat(response.readEntity(JsonNode.class).get("id").asText()).isEqualTo("release-1");
		}

		assertThat(NEWS_SERVICE.getNews(null)).containsExactly(NEWS_ITEM);
	}

	@Test
	void reportsDuplicateNewsItemAsBadRequest() throws IOException {
		NEWS_SERVICE.addNewsItem(NEWS_ITEM);

		try(Response response = EXT.target("/news").request()
				.post(Entity.entity(newsJson(NEWS_ITEM), MediaType.APPLICATION_JSON_TYPE))) {
			assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
			assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
			assertThat(response.readEntity(String.class)).contains("Item with id release-1 already exists");
		}
	}

	@Test
	void validatesNewsItemAtResourceBoundary() {
		NewsItem invalid = new NewsItem(
				"INVALID ID",
				"",
				"The first release",
				null,
				LocalDate.of(2026, 9, 30),
				Set.of()
		);

		try(Response response = EXT.target("/news").request()
				.post(Entity.entity(newsJson(invalid), MediaType.APPLICATION_JSON_TYPE))) {
			JsonNode error = assertValidationError(response);
			assertThat(error.get("message").asText())
					.contains("id", "title");
		}
	}

	@Test
	void rejectsNullNewsItemAtResourceBoundary() {
		try(Response response = EXT.target("/news").request()
				.post(Entity.entity("null", MediaType.APPLICATION_JSON_TYPE))) {
			JsonNode error = assertValidationError(response);
			assertThat(error.get("message").asText()).contains("addNewsItem.newsItem");
		}
	}

	@Test
	void removesNewsItem() throws IOException {
		NEWS_SERVICE.addNewsItem(NEWS_ITEM);

		try(Response response = EXT.target("/news/release-1").request().delete()) {
			assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
			assertThat(response.hasEntity()).isFalse();
		}

		assertThat(NEWS_SERVICE.getNews(null)).isEmpty();
	}

	@Test
	void reportsUnknownNewsItemAsNotFound() {
		try(Response response = EXT.target("/news/unknown").request().delete()) {
			assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
			assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
			assertThat(response.readEntity(String.class)).contains("No news item with id 'unknown' exists");
		}
	}

	private static JsonNode assertValidationError(Response response) {
		assertThat(response.getStatus()).isEqualTo(422);
		assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
		JsonNode error = response.readEntity(JsonNode.class);
		assertThat(error.get("code").asInt()).isEqualTo(422);
		assertThat(error.get("message").asText()).isNotBlank();
		return error;
	}

	private static String newsJson(NewsItem item) {
		return """
				{
				  "id": "%s",
				  "title": "%s",
				  "description": "%s",
				  "link": %s,
				  "date": "%s",
				  "categories": ["dataset-a"]
				}
				""".formatted(
				item.id(),
				item.title(),
				item.description(),
				item.link() == null ? "null" : "\"" + item.link() + "\"",
				item.date()
		);
	}

	private static final class InMemoryNewsService implements NewsService {

		private final Map<String, NewsItem> news = new LinkedHashMap<>();

		@Override
		public void reloadNews() {
		}

		@Override
		public List<NewsItem> getNews(String category) {
			return news.values().stream()
					.filter(item -> category == null || item.categories().contains(category))
					.toList();
		}

		@Override
		public void addNewsItem(NewsItem item) {
			if(news.putIfAbsent(item.id(), item) != null) {
				throw new IllegalArgumentException("Item with id %s already exists".formatted(item.id()));
			}
		}

		@Override
		public boolean removeNewsItem(String id) {
			return news.remove(id) != null;
		}

		@Override
		public void close() {
		}

		private void clear() {
			news.clear();
		}
	}
}
