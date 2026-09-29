import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.*;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileNewsServiceTest {

	@TempDir
	private static Path newsFolder;
	private static NewsService newsService;

	@BeforeAll
	public static void setUp() {
		newsService = new FileSystemNewsService(newsFolder);
	}


	@Order(0)
	@Test
	void addNews() {
		assertThatCode(() ->
		newsService.addNewsItem(new NewsItem("id","title", "description", URI.create("/read/more"), LocalDate.of(2026,9,1), Set.of("cat1")))).doesNotThrowAnyException();

		assertThat(newsFolder.resolve("2026-09-01_id.json")).exists();

	}

	@Order(1)
	@Test
	void testListNews() {
		newsService.getNews(null);
	}

}
