package com.bakdata.conquery.news;

import static com.bakdata.conquery.news.NewsService.ID_PATTERN;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


/**
 * Filesystem representation of a news item. The date is derived from the filename.
 */
public record FileSystemNewsItem(
		@NotBlank @Pattern(regexp = ID_PATTERN) String id,
		@NotBlank String title,
		@NotBlank String description,
		URI link,
		Set<@NotBlank String> categories
) {
	public FileSystemNewsItem {
		categories = categories == null ? Collections.emptySet() : Set.copyOf(categories);
	}
}
