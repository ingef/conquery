package com.bakdata.conquery.news;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NewsPublicationPolicy {

	private final Clock clock;

	public NewsPublicationStatus getStatus(NewsItem item) {
		if (item.date().isAfter(LocalDate.now(clock))) {
			return NewsPublicationStatus.SCHEDULED;
		}
		return NewsPublicationStatus.PUBLISHED;
	}

	public boolean isPublished(NewsItem item) {
		return getStatus(item) == NewsPublicationStatus.PUBLISHED;
	}
}
