import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

public class FileSystemNewsService implements NewsService {

	private final FileChannel lockFile;

	private final Path newsFolder;
	private final ObjectReader newsItemReader;
	private final ObjectWriter newsItemWriter;

	public FileSystemNewsService(Path newsFolder) {
		this.newsFolder = newsFolder;

		Path lockfilePath = newsFolder.resolve(".lock");
		try{

			this.lockFile = FileChannel.open(
					lockfilePath,
					StandardOpenOption.CREATE,
					StandardOpenOption.WRITE
			);
		} catch (IOException e) {
			throw new RuntimeException("Unable to open lock file %s".formatted(lockfilePath), e);
		}

		final ObjectMapper om = new ObjectMapper();
		this.newsItemReader = om.readerFor(FileSystemNewsItem.class);
		this.newsItemWriter = om.writerFor(FileSystemNewsItem.class);
	}

	@Override
	public List<NewsItem> getNews(String category) {
		return List.of();
	}

	@Override
	public void addNewsItem(NewsItem news) throws IOException {
		try(final FileLock ignored = lockFile.tryLock()) {
			// TODO add attributes?
			Path dateFolder = Files.createDirectories(newsFolder.resolve(news.date().toString()));

			FileSystemNewsItem fsNews = new FileSystemNewsItem(
					news.title(),
					news.description(),
					news.link(),
					news.categories()
			);
			String fsSafeId = news.id()
						.trim().toLowerCase(Locale.ROOT).replace("[^a-zA-Z0-9_-]", "-");
			File newsFile = dateFolder.resolve(fsSafeId + ".json").toFile();

			newsItemWriter.writeValue(newsFile, fsNews);

		}
	}

	private List<NewsItem> registerNewsRoot() throws IOException {
		Map<String, NewsItem> newsItems = new HashMap<>();
		Files.walkFileTree(newsFolder, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(
					Path dir,
					BasicFileAttributes attrs) throws IOException {
				register(dir, newsItems);
				return FileVisitResult.CONTINUE;
			}
		});

		return List.of();

	}

	private void register(Path dateFolder, Map<String, NewsItem> collectedNewsItems) throws IOException {
		Files.walkFileTree(dateFolder, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				try (BufferedReader reader = Files.newBufferedReader(file)) {
					FileSystemNewsItem item = newsItemReader.readValue(reader);
				}
				return FileVisitResult.CONTINUE;

			}
		});
	}
}
