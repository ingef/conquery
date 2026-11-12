package news;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import com.bakdata.conquery.news.FileSystemNewsService;
import com.bakdata.conquery.news.NewsItem;
import com.bakdata.conquery.news.NewsService;
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

	// TODO after JUnit 5.14.4 use Arguments.ArumentSet for better test rendering
	static Stream<Arguments> goodNews() {
		return Stream.of(
				Arguments.of("News without published datasets", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\"}", 1),
				Arguments.of("News with link", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"link\":\"/read/more\"}", 1),
				Arguments.of("News with published datasets null", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"publishedToDatasets\": null}", 1),
				Arguments.of("News with single published dataset", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"publishedToDatasets\":[\"some_dataset\"]}", 1),
				Arguments.of("News with multiple published datasets", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"publishedToDatasets\":[\"first_dataset\", \"second_dataset\"]}", 1),
				Arguments.of("News id mismatch with filename", "2026-08-23_some_news.json", "{\"id\":\"other_news\", \"title\": \"title\", \"description\":\"some description\"}", 1) // Should print a warning
		);

	}

	static Stream<Arguments> badNews() {
		return Stream.of(
				Arguments.of("News with broken link uri", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"description\":\"some description\", \"link\":\"http://example .com\"}", 0),
				Arguments.of("News with wrong date", "2026-13-23_some_news.json", "{\"id\":\"some_news\", \"description\":\"some description\"}", 0),
				Arguments.of("News with empty content", "2026-08-23_some_news.json", "", 0),
				Arguments.of("News with broken json", "2026-08-23_some_news.json", "{\"id\":\"some_news}", 0),
				Arguments.of("News missing id", "2026-08-23_some_news.json", "{\"title\": \"title\", \"description\":\"some description\", \"publishedToDatasets\":[\"first_dataset\", \"second_dataset\"]}", 0),

				Arguments.of("News missing title", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"description\":\"some description\", \"publishedToDatasets\":[\"first_dataset\", \"second_dataset\"]}", 0),

				Arguments.of("News missing description", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"publishedToDatasets\":[\"first_dataset\", \"second_dataset\"]}", 0),

				Arguments.of("News with blank published dataset", "2026-08-23_some_news.json", "{\"id\":\"some_news\", \"title\": \"title\", \"description\":\"some description\", \"publishedToDatasets\":[\"first_dataset\", \"\"]}", 0)
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
	void loadNews(String _description, String fileName, String content, int loaded) throws IOException {
		File newsFile = newsFolder.resolve(fileName).toFile();

		try (FileWriter fileWriter = new FileWriter(newsFile)) {
			fileWriter.write(content);
		}

		assertThatCode(() -> newsService.reloadNews()).doesNotThrowAnyException();
		assertThat(newsService.getNews(null)).hasSize(loaded);
		assertThat(newsFile.delete()).as("Deleting %s".formatted(newsFile)).isTrue();
	}

	@Test
	void ignoresLegacyCategoriesWhenLoadingNews(@TempDir Path legacyNewsFolder) throws Exception {
		final File legacyNewsFile = legacyNewsFolder.resolve("2026-08-23_legacy.json").toFile();
		try (final FileWriter fileWriter = new FileWriter(legacyNewsFile)) {
			fileWriter.write("{\"id\":\"legacy\",\"title\":\"Legacy\",\"description\":\"Description\",\"categories\":[\"dataset-a\"]}");
		}

		try (final NewsService legacyNewsService = new FileSystemNewsService(legacyNewsFolder, validatorFactory.getValidator())) {
			assertThat(legacyNewsService.getNews(null)).singleElement()
					.extracting(NewsItem::publishedToDatasets)
					.isEqualTo(Set.of());
			assertThat(legacyNewsService.getNews("dataset-a")).isEmpty();
		}
	}


	@Order(1)
	@Test
	void addNews() {
		NewsItem newsItem1 = new NewsItem("id1", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 9, 1), Set.of("cat1"));
		NewsItem newsItem2 = new NewsItem("ID2", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat1"));
		NewsItem newsItem3 = new NewsItem("id3", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat3"));
		NewsItem newsItem4 = new NewsItem("id4", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of());
		NewsItem newsItem5 = new NewsItem("id5", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), null);


		assertThatCode(() -> newsService.addNewsItem(newsItem1)).doesNotThrowAnyException();
		assertThatThrownBy(() -> newsService.addNewsItem(newsItem2)).isInstanceOf(ConstraintViolationException.class).hasMessageContaining(NewsService.ID_PATTERN);
		assertThatCode(() -> newsService.addNewsItem(newsItem3)).doesNotThrowAnyException();
		assertThatCode(() -> newsService.addNewsItem(newsItem4)).doesNotThrowAnyException();
		assertThatCode(() -> newsService.addNewsItem(newsItem5)).doesNotThrowAnyException();

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
				new NewsItem("id3", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat3")),
				new NewsItem("id4", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of()),
				new NewsItem("id5", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of()),
				new NewsItem("id1", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 9, 1), Set.of("cat1"))
		);


		final List<NewsItem> newsCat1 = newsService.getNews("cat1");

		assertThat(newsCat1).containsExactly(
				new NewsItem("id1", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 9, 1), Set.of("cat1"))
		);


		final List<NewsItem> newsCat3 = newsService.getNews("cat3");

		assertThat(newsCat3).containsExactly(
				new NewsItem("id3", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat3"))
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
				new NewsItem("id3", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat3")),
		new NewsItem("id4", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of()),
				new NewsItem("id5", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of())
		);


		final List<NewsItem> newsCat1 = newsService.getNews("cat1");

		assertThat(newsCat1).isEmpty();


		final List<NewsItem> newsCat3 = newsService.getNews("cat3");

		assertThat(newsCat3).containsExactly(
				new NewsItem("id3", "title", "description", URI.create("/read/more"), LocalDate.of(2026, 8, 1), Set.of("cat3"))
		);
	}

	@AfterAll
	static void tearDown() throws Exception {
		newsService.close();
		validatorFactory.close();
	}

}
