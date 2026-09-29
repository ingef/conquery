import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


/**
 * Filesystem representation of a news item.
 */
public record FileSystemNewsItem(
		@NotBlank String id,
		@NotBlank String title,
		@NotBlank String description,
		@Valid URI link,
		@NotNull LocalDate date,
		Set<@NotBlank String> categories
) {
	public FileSystemNewsItem {
		category = category == null ? new HashSet<>() : category;
	}
}
