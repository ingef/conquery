import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

import lombok.Getter;

public record NewsItem(
		String id,
		String title,
		String description,
		URI link,
		LocalDate date,
		Set<String> categories) {
}
