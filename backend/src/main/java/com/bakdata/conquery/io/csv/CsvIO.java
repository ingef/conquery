package com.bakdata.conquery.io.csv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvWriter;

public class CsvIO {

	public static CsvParser createParser() {
		return new CsvParser(ConqueryConfig.getInstance().getCsv().createCsvParserSettings());
	}

	public static CsvWriter createWriter() {
		return new CsvWriter(ConqueryConfig.getInstance().getCsv().createCsvWriterSettings());
	}

	public static boolean isGZipped(File file) throws IOException {
		return Files.probeContentType(file.toPath()).equalsIgnoreCase("application/x-gzip");
	}
}
