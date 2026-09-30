package news;

import static news.NewsService.ID_PATTERN;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NewsItem(
		@NotBlank @Pattern(regexp = ID_PATTERN) String id,
		@NotBlank String title,
		@NotBlank String description,
		URI link,
		@NotNull LocalDate date,
		Set<@NotBlank String> categories) {

	public NewsItem {
		categories = categories == null ? Set.of() : Set.copyOf(categories);
	}
}
