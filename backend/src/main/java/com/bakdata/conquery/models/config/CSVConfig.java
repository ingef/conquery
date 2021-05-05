package com.bakdata.conquery.models.config;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.validation.constraints.NotNull;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.*;
import org.hibernate.validator.constraints.Length;

/**
 * Holds the necessary information to configure CSV parsers and writers. 
 */
@Getter @Setter @With @AllArgsConstructor @NoArgsConstructor
public class CSVConfig {
	private char escape = '\\';
	private char comment = '\0';
	private char delimeter = ',';
	@Length(min=1, max=2) @NotNull
	private String lineSeparator = "\n";
	private char quote = '"';
	@NotNull
	private Charset encoding = StandardCharsets.UTF_8;
	private boolean skipHeader = false;
	private boolean parseHeaders = true;
	private int maxColumns = 1_000_000; // This should be sufficiently large.
	
	/**
	 * Helper method to generate parser settings from the provided options in this class.
	 * @return Setting object that can be passed into a {@link CsvParser}.
	 */
	private CsvParserSettings createCsvParserSettings() {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setFormat(createCsvFormat());
		settings.setHeaderExtractionEnabled(parseHeaders);
		settings.setMaxColumns(maxColumns);
		settings.setLineSeparatorDetectionEnabled(true);
		return settings;
	}

	/**
	 * Helper method to generate writer settings from the provided options in this class.
	 * @return Setting object that can be passed into a {@link CsvWriter}.
	 */
	private CsvWriterSettings createCsvWriterSettings() {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setMaxColumns(maxColumns);
		settings.setFormat(createCsvFormat());
		return settings;
	}
	
	/**
	 * Helper method to generate format settings from the provided options in this class.
	 * @return Format object that can be passed into {@link CsvWriterSettings} and {@link CsvParserSettings}.
	 */
	private CsvFormat createCsvFormat() {
		CsvFormat format = new CsvFormat();
		format.setQuoteEscape(getEscape());
		format.setCharToEscapeQuoteEscaping(getEscape());
		format.setComment(getComment());
		format.setDelimiter(getDelimeter());
		format.setLineSeparator(getLineSeparator());
		format.setQuote(getQuote());
		return format;
	}

	/**
	 * Creates a new CSV parser using the global settings from {@link ConqueryConfig}.
	 * @return The newly created parser.
	 */
	public CsvParser createParser() {
		return new CsvParser(createCsvParserSettings());
	}

	/**
	 * Creates a new CSV writer using the global settings from {@link ConqueryConfig}.
	 * @return The newly created writer.
	 */
	public CsvWriter createWriter() {
		return new CsvWriter(createCsvWriterSettings());
	}

	/**
	 * Creates a new CSV writer using the global settings from {@link ConqueryConfig} and an existing writer object to write through.
	 * @param writer The writer to write through.
	 * @return The newly created writer.
	 */
	public CsvWriter createWriter(@NonNull Writer writer) {
		return new CsvWriter(writer, createCsvWriterSettings());
	}

}