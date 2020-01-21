package com.bakdata.conquery.io.csv;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvWriter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass @Slf4j
public class CsvIo {

	public static CsvParser createParser() {
		return new CsvParser(ConqueryConfig.getInstance().getCsv().createCsvParserSettings());
	}

	public static CsvWriter createWriter() {
		return new CsvWriter(ConqueryConfig.getInstance().getCsv().createCsvWriterSettings());
	}

	public static boolean isGZipped(File file) throws IOException {
		return Files.probeContentType(file.toPath()).contains("gzip");
	}
}
