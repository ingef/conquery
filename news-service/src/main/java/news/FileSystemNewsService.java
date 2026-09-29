package news;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;

import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;

@Slf4j
public class FileSystemNewsService implements NewsService {

	public static final Pattern FILENAME_PATTERN = Pattern.compile("^(?<date>\\d{4}-\\d{2}-\\d{2})_(?<id>[a-z0-9\\-_]+)\\.json$");
	public static final Pattern FILE_SAFE_PATTERN = Pattern.compile("[^a-z0-9_-]");

	public final Validator validator;

	private final Path newsFolder;
	@Getter(onMethod_ = @VisibleForTesting)
	private final ObjectReader newsItemReader;
	@Getter(onMethod_ = @VisibleForTesting)
	private final ObjectWriter newsItemWriter;
	private final FileLock lock;
	private final FileChannel lockFile;

	private volatile Map<String, NewsItemContainer> news;

	public FileSystemNewsService(Path newsFolder, Validator validator) throws IOException {
		this.newsFolder = newsFolder;
		this.validator = validator;

		final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
		this.newsItemReader = om.readerFor(FileSystemNewsItem.class);
		this.newsItemWriter = om.writerFor(FileSystemNewsItem.class);

		Path lockfilePath = newsFolder.resolve(".lock");
		try{

			lockFile = FileChannel.open(
					lockfilePath,
					StandardOpenOption.CREATE,
					StandardOpenOption.WRITE
			);

			this.lock = lockFile.lock();

			reloadNews();
		} catch (Throwable setupExeption) {
			try {

				close();
			} catch (Throwable cleanupFailure) {
				setupExeption.addSuppressed(cleanupFailure);
			}
			// Rethrow
			throw setupExeption;
		}

	}

	@Override
	public List<NewsItem> getNews(String category) {
		return news.values().stream()
				.filter(c -> category == null || c.newsItem().categories().contains(category))
				.sorted(Comparator.comparing(NewsItemContainer::file))
				.map(NewsItemContainer::newsItem).toList();
	}

	@Override
	public synchronized void addNewsItem(NewsItem newsItem) throws IOException {

		validate(newsItem);

		if (news.get(newsItem.id()) != null) {
			throw new IllegalArgumentException("Item with id %s already exist".formatted(newsItem.id()));
		}

		String fsSafeId = makeFileSystemSafeId(newsItem.id());
		Path targetNewsPath = newsFolder.resolve(newsItem.date() + "_" + fsSafeId + ".json");
		File targetNewsFile = targetNewsPath.toFile();
		if (!FILENAME_PATTERN.matcher(targetNewsPath.getFileName().toString()).matches()) {
			// Should not happen
			throw new RuntimeException("Invalid file name format. Derived file name does note match pattern: %s".formatted(targetNewsFile.getName()));
		}

		if (targetNewsFile.exists()) {
			throw new FileAlreadyExistsException(targetNewsFile.toString());
		}

		FileSystemNewsItem fsNews = new FileSystemNewsItem(
				newsItem.id(),
				newsItem.title(),
				newsItem.description(),
				newsItem.link(),
				newsItem.categories()
		);

		try( TemporaryFile temporary = TemporaryFile.create(newsFolder)) {
			newsItemWriter.writeValue(temporary.path().toFile(), fsNews);

			temporary.moveTo(targetNewsPath);
		}

		log.info("Added news item to folder {}", targetNewsFile.getAbsolutePath());

		reloadNews();
	}

	@Override
	public synchronized boolean removeNewsItem(String id) throws IOException {
		NewsItemContainer newsItemContainer = news.get(id);
		if (newsItemContainer == null) {
			log.warn("Could not remove news item. Id '{}' is unknown", id);
			return false;
		}

		boolean deleted = Files.deleteIfExists(newsItemContainer.file);

		reloadNews();

		return deleted;
	}

	private static @NonNull String makeFileSystemSafeId(String id) {
        return FILE_SAFE_PATTERN.matcher(id.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
	}

	public synchronized void reloadNews() throws IOException {
		news = loadNews();
	}

	private Map<String, NewsItemContainer> loadNews() throws IOException {
		Map<String, NewsItemContainer> newsItems = new HashMap<>();

		// Ignore sub-directories
		try(Stream<Path> stream = Files.list(newsFolder)) {
			List<Path> fileList = stream.toList();
			for (Path path : fileList) {
				Path fileName = path.getFileName();
				Matcher matcher = FILENAME_PATTERN.matcher(fileName.toString());
				if (!matcher.matches()) {
					log.debug("Ignoring file {}", fileName);
					continue;
				}

				File file = path.toFile();
				if (file.isDirectory()) {
					log.warn("Ignoring directory {}", file);
					continue;
				}
				if (!file.canRead()) {
					log.warn("Ignoring unreadable file {}", file);
					continue;
				}

				log.trace("Loading file {}", fileName);

				String dateString = matcher.group("date");
				String fsSafeId = matcher.group("id");
				try {
					LocalDate date = LocalDate.parse(dateString);
					NewsItem newsItem = readNewsItem(path, date);

					String expectedFileNameId = makeFileSystemSafeId(newsItem.id());
					if (!expectedFileNameId.equals(fsSafeId)) {
						log.warn("Derived id in file name of {} does not match id in content. Content id: '{}'; Actual file name id: '{}'; Expected file name id: '{}'", path, newsItem.id(), fsSafeId, expectedFileNameId);
					}

					NewsItemContainer previous = newsItems.putIfAbsent(newsItem.id(), new NewsItemContainer(path, newsItem));

					if (previous != null) {
						throw new IOException(
								"Duplicate news id '%s' in %s and %s".formatted(newsItem.id(), previous.file(), path)
						);
					}

				} catch (DateTimeParseException e) {
					log.error("Ignoring news item {}. Unable to parse date string {}", path, dateString, e);
					continue;
				} catch (JacksonException e) {
					log.error("Ignoring news item {}. Unable to parse content", path, e);
					continue;
				} catch (ConstraintViolationException e) {
					log.error("Ignoring news item {}. Validation failed", path, e);
					continue;
				}

				log.debug("Loaded file {}", fileName);
			}

		}

		return newsItems;

	}

	private NewsItem readNewsItem(Path file, LocalDate date) throws IOException {

		try (BufferedReader reader = Files.newBufferedReader(file)) {
			FileSystemNewsItem item = newsItemReader.readValue(reader);

			validate(item);

			return new NewsItem(
					item.id(),
					item.title(),
					item.description(),
					item.link(),
					date,
					item.categories()
			);

		}
	}

	@Override
	public void close() throws IOException {
		if (lockFile != null) {
			lockFile.close();
		}
	}

	private <T> void validate(T value) {
		var violations = validator.validate(value);

		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
	}

	private record NewsItemContainer(Path file, NewsItem newsItem) {}
}
