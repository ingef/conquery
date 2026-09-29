package news;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileNewsServiceTest {

	@TempDir
	private static Path newsFolder;
	private static NewsService newsService;
	private static ValidatorFactory validatorFactory;

	@BeforeAll
	public static void setUp() throws IOException {
		validatorFactory = Validation
				.byDefaultProvider()
				.configure()
				.messageInterpolator(new ParameterMessageInterpolator())
				.buildValidatorFactory();
		Validator validator = validatorFactory.getValidator();
		newsService = new FileSystemNewsService(newsFolder, validator);
	}

	static Stream<Arguments.ArgumentSet> goodNews() {
		return Stream.of(
				argumentSet("News without category", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\"}", 1),
				argumentSet("News with link", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"link\":\"/read/more\"}", 1),
				argumentSet("News with category null", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"categories\": null}", 1),
				argumentSet("News with single category", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"categories\":[\"some_category\"]}", 1),
				argumentSet("News with multiple categories", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"categories\":[\"first_category\", \"second_category\"]}", 1),
				argumentSet("News id mismatch with filename", "2026-08-23_some_news.json", "{\"id\":\"other_news\", \"title\": \"title\", \"description\":\"some description\"}", 1) // Should print a warning
		);

	}

	static Stream<Arguments.ArgumentSet> badNews() {
		return Stream.of(
				argumentSet("News with broken link uri", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"description\":\"some description\", \"link\":\"http://example .com\"}", 0),
				argumentSet("News with wrong date", "2026-13-23_some_news.json", "{\"id\":\"some_news\", \"description\":\"some description\"}", 0),
				argumentSet("News with empty content", "2026-08-23_some_news.json", "", 0),
				argumentSet("News with broken json", "2026-08-23_some_news.json", "{\"id\":\"some_news}", 0),
				argumentSet("News missing id", "2026-08-23_some_news.json", "{\"title\": \"title\", \"description\":\"some description\", \"categories\":[\"first_category\", \"second_category\"]}", 0),

				argumentSet("News missing title", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"description\":\"some description\", \"categories\":[\"first_category\", \"second_category\"]}", 0),

				argumentSet("News missing description", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"categories\":[\"first_category\", \"second_category\"]}", 0),

				argumentSet("News with blank category", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"categories\":[\"first_category\", \"\"]}", 0)
		);

	}

	static Stream<Arguments> prepopulatedNews() {
		return Stream.concat(
				goodNews(),
				badNews()
		);
	}

	@Order(0)
	@ParameterizedTest
	@MethodSource("prepopulatedNews")
	void loadNews(String fileName, String content, int loaded) throws IOException {
		File newsFile = newsFolder.resolve(fileName).toFile();

		try(FileWriter fileWriter = new FileWriter(newsFile)) {
			fileWriter.write(content);
		}

		assertThatCode(() -> newsService.reloadNews()).doesNotThrowAnyException();
		assertThat(newsService.getNews(null)).hasSize(loaded);
		assertThat(newsFile.delete()).as("Deleting %s".formatted(newsFile)).isTrue();
	}


	@Order(1)
	@Test
	void addNews() {
		NewsItem newsItem1 = new NewsItem("id1", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 9, 1), Set.of("cat1"));
		NewsItem newsItem2 = new NewsItem("ID2", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat1"));
		NewsItem newsItem3 = new NewsItem("id3", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat3"));


		assertThatCode(() -> newsService.addNewsItem(newsItem1)).doesNotThrowAnyException();
		assertThatThrownBy(() -> newsService.addNewsItem(newsItem2)).isInstanceOf(ConstraintViolationException.class).hasMessageContaining(NewsService.ID_PATTERN);
		assertThatCode(() -> newsService.addNewsItem(newsItem3)).doesNotThrowAnyException();

		assertThat(newsFolder.resolve("2026-09-01_id1.json")).exists();
		assertThat(newsFolder.resolve("2026-08-01_id2.json")).doesNotExist();
		assertThat(newsFolder.resolve("2026-08-01_id3.json")).exists();

		assertThatThrownBy(() -> newsService.addNewsItem(newsItem1)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("already exist");

	}

	@Order(2)
	@Test
	void testListNews() {
		List<NewsItem> news = newsService.getNews(null);

		assertThat(news).containsExactly(
				new NewsItem("id3", "title","description", URI.create("/read/more"), LocalDate.of(2026,8,1), Set.of("cat3")),
				new NewsItem("id1", "title","description", URI.create("/read/more"), LocalDate.of(2026,9,1), Set.of("cat1"))
		);


		List<NewsItem> newsCat1 = newsService.getNews("cat1");

		assertThat(newsCat1).containsExactly(
				new NewsItem("id1", "title","description", URI.create("/read/more"), LocalDate.of(2026,9,1), Set.of("cat1"))
		);


		List<NewsItem> newsCat3 = newsService.getNews("cat3");

		assertThat(newsCat3).containsExactly(
				new NewsItem("id3", "title","description", URI.create("/read/more"), LocalDate.of(2026,8,1), Set.of("cat3"))
		);
	}

	@Order(3)
	@Test
	void removeNews() {
		assertThatCode(() -> assertThat(newsService.removeNewsItem("id1")).isTrue()).doesNotThrowAnyException();

		assertThatCode(() -> assertThat(newsService.removeNewsItem("unknown_id")).isFalse()).doesNotThrowAnyException();
	}

	@Order(4)
	@Test
	void testListNews2() {
		List<NewsItem> news = newsService.getNews(null);

		assertThat(news).containsExactly(
				new NewsItem("id3", "title","description", URI.create("/read/more"), LocalDate.of(2026,8,1), Set.of("cat3"))
		);


		List<NewsItem> newsCat1 = newsService.getNews("cat1");

		assertThat(newsCat1).containsExactly(
		);


		List<NewsItem> newsCat3 = newsService.getNews("cat3");

		assertThat(newsCat3).containsExactly(
				new NewsItem("id3", "title","description", URI.create("/read/more"), LocalDate.of(2026,8,1), Set.of("cat3"))
		);
	}

	@AfterAll
	static void tearDown() throws Exception {
		newsService.close();
		validatorFactory.close();
	}

}
