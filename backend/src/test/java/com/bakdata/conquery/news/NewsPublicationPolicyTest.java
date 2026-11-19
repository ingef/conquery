package com.bakdata.conquery.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import org.junit.jupiter.api.Test;

class NewsPublicationPolicyTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
	private static final Clock CLOCK = Clock.fixed(
			Instant.parse("2026-10-27T12:00:00Z"),
			ZoneId.of("Europe/Berlin")
	);
	private final NewsPublicationPolicy policy = new NewsPublicationPolicy(CLOCK);

	@Test
	void publishesPastNews() {
		final NewsItem item = newsItem(TODAY.minusDays(1));

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.PUBLISHED);
		assertThat(policy.isPublished(item)).isTrue();
	}

	@Test
	void publishesNewsDatedToday() {
		final NewsItem item = newsItem(TODAY);

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.PUBLISHED);
		assertThat(policy.isPublished(item)).isTrue();
	}

	@Test
	void schedulesFutureNews() {
		final NewsItem item = newsItem(TODAY.plusDays(1));

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.SCHEDULED);
		assertThat(policy.isPublished(item)).isFalse();
	}

	@Test
	void marksNewsWithoutPublishedDatasetsAsUnpublished() {
		final NewsItem item = newsItem(TODAY, Set.of());

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.UNPUBLISHED);
		assertThat(policy.isPublished(item)).isFalse();
	}

	@Test
	void unpublishedTakesPrecedenceOverScheduled() {
		final NewsItem item = newsItem(TODAY.plusDays(1), Set.of());

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.UNPUBLISHED);
	}

	private static NewsItem newsItem(LocalDate date) {
		return newsItem(date, Set.of("dataset-a"));
	}

	private static NewsItem newsItem(LocalDate date, Set<String> publishedToDatasets) {
		return new NewsItem("news", "News", "Description", null, date, publishedToDatasets);
	}
}
