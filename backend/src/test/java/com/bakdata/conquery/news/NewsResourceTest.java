package com.bakdata.conquery.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import com.bakdata.conquery.models.auth.entities.Subject;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import org.junit.jupiter.api.Test;

class NewsResourceTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
	private static final NewsPublicationPolicy PUBLICATION_POLICY = new NewsPublicationPolicy(Clock.fixed(
			Instant.parse("2026-10-27T12:00:00Z"),
			ZoneId.of("Europe/Berlin")
	));

	@Test
	void returnsOnlyPublishedNewsForTheRequestedDataset() {
		RecordingNewsService newsService = new RecordingNewsService(List.of(
				newsItem("past", TODAY.minusDays(1)),
				newsItem("today", TODAY),
				newsItem("future", TODAY.plusDays(1))
		));
		NewsResource resource = new NewsResource(newsService, PUBLICATION_POLICY);
		resource.setDatasetId(new DatasetId("dataset-a"));

		var result = resource.getNews(mock(Subject.class));

		assertThat(result).extracting(FrontendNewsItem::id).containsExactly("past", "today");
		assertThat(newsService.requestedCategory).isEqualTo("dataset-a");
	}

	private static NewsItem newsItem(String id, LocalDate date) {
		return new NewsItem(id, id, "Description", null, date, Set.of("dataset-a"));
	}

	private static final class RecordingNewsService implements NewsService {

		private final List<NewsItem> news;
		private String requestedCategory;

		private RecordingNewsService(List<NewsItem> news) {
			this.news = news;
		}

		@Override
		public void reloadNews() {
		}

		@Override
		public List<NewsItem> getNews(String category) {
			requestedCategory = category;
			return news;
		}

		@Override
		public void addNewsItem(NewsItem item) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeNewsItem(String id) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
		}
	}
}
