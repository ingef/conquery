package news;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

import static news.NewsService.ID_PATTERN;

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
