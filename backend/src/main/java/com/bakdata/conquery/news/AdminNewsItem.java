package com.bakdata.conquery.news;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

public record AdminNewsItem(
		String id,
		String title,
		String description,
		URI link,
		LocalDate date,
		Set<String> categories,
		NewsPublicationStatus publicationStatus) {

	public static AdminNewsItem from(NewsItem item, NewsPublicationStatus publicationStatus) {
		return new AdminNewsItem(
				item.id(),
				item.title(),
				item.description(),
				item.link(),
				item.date(),
				item.categories(),
				publicationStatus
		);
	}
}
