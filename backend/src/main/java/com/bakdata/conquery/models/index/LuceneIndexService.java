package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.io.FileUtil;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.univocity.parsers.csv.CsvParser;
import io.dropwizard.lifecycle.Managed;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jetbrains.annotations.NotNull;

@Slf4j
public class LuceneIndexService implements Managed {


	private Path indexDir = Path.of("./storage/index");
	private ConqueryConfig config;

	// Todo might needs synchronization
	/**
	 * Hash pof original csv to search index
	 */
	private final Map<String, IndexSearcher> indexes = new HashMap<>();
	private final List<DirectoryReader> readers = new ArrayList<>();
	private final List<Directory> directories = new ArrayList<>();

	// Path to hash
	private final LoadingCache<Path, String> fileHash = CacheBuilder.newBuilder().build(new CacheLoader<Path, String>() {
		@Override
		public String load(@NotNull Path path) throws Exception {
			return FileUtil.getFileChecksum(path.toFile());
		}
	});

	@SneakyThrows(IOException.class)
	void loadIndexes() {
		final List<Path> dirs = Files.list(indexDir).filter(Files::isDirectory).collect(Collectors.toList());

		for (Path dir : dirs) {
			final FSDirectory directory = FSDirectory.open(dir);
			DirectoryReader reader = DirectoryReader.open(directory);
			// The name of the folder is the hash of the original folder
			directories.add(directory);
			readers.add(reader);
			indexes.put(dir.getFileName().toString(), new IndexSearcher(reader));
		}
	}

	/**
	 * Use sha hash to remember indexed files
	 *
	 * @param csv
	 * @throws IOException
	 */
	public String createIndexFromCsv(Path csv) throws IOException {
		//SHA-1 checksum
		String shaChecksum = fileHash.getUnchecked(csv);

		if (indexes.containsKey(shaChecksum)) {
			log.debug("Index for {} was already created", csv);
			return shaChecksum;
		}

		// creates a CSV parser
		CsvParser parser = config.getCsv().createParser();

		List<String[]> allRows = parser.parseAll(csv.toFile());
		final String[] headers = parser.getContext().headers();

		Analyzer analyzer = new StandardAnalyzer();


		int nullCount = 0;
		Path indexPath = indexDir.resolve(shaChecksum);
		if (!indexPath.toFile().mkdir()) {
			throw new IllegalStateException("Unable to create folder: " + indexPath);
		}
		Directory directory = FSDirectory.open(indexPath);
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter iwriter = new IndexWriter(directory, config);
		for (int i = 1; i < allRows.size(); i++) {
			Document doc = new Document();
			// Create a field for every header
			for (int j = 0; j < headers.length; j++) {
				String header = headers[j];
				final String value = allRows.get(i)[j];
				if (value == null) {
					nullCount++;
					continue;
				}
				doc.add(new Field(header, value, TextField.TYPE_STORED));
			}
			iwriter.addDocument(doc);
		}
		iwriter.close();

		DirectoryReader ireader = DirectoryReader.open(directory);

		readers.add(ireader);

		indexes.put(shaChecksum, new IndexSearcher(ireader));

		return shaChecksum;
	}

	IndexSearcher getIndexSearcher(Path csv) {
		return indexes.get(fileHash.getUnchecked(csv));
	}


	@Override
	public void start() throws Exception {
		loadIndexes();
	}

	@Override
	public void stop() throws Exception {
		for (DirectoryReader reader : readers) {
			reader.close();
		}
		for (Directory directory : directories) {
			directory.close();
		}
	}
}
