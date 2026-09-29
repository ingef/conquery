import java.net.URI;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;


/**
 * Filesystem representation of a news item. It does not hold the date because we infer that from the folder it is in.
 * @param title
 * @param description
 * @param link
 * @param category
 */
public record FileSystemNewsItem(
		@NotBlank String title,
		@NotBlank String description,
		@Valid URI link,
		Set<@NotBlank String> category
) {
	public FileSystemNewsItem {
		category = category == null ? new HashSet<>() : category;
	}
}
