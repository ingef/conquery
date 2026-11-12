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
		NewsItem item = newsItem(TODAY.minusDays(1));

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.PUBLISHED);
		assertThat(policy.isPublished(item)).isTrue();
	}

	@Test
	void publishesNewsDatedToday() {
		NewsItem item = newsItem(TODAY);

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.PUBLISHED);
		assertThat(policy.isPublished(item)).isTrue();
	}

	@Test
	void schedulesFutureNews() {
		NewsItem item = newsItem(TODAY.plusDays(1));

		assertThat(policy.getStatus(item)).isEqualTo(NewsPublicationStatus.SCHEDULED);
		assertThat(policy.isPublished(item)).isFalse();
	}

	private static NewsItem newsItem(LocalDate date) {
		return new NewsItem("news", "News", "Description", null, date, Set.of());
	}
}
