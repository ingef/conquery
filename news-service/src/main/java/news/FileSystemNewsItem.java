package news;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static news.NewsService.ID_PATTERN;


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
		categories = categories == null ? new HashSet<>() : Set.copyOf(categories);
	}
}
