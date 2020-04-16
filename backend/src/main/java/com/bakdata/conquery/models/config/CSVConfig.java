package com.bakdata.conquery.models.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.validation.constraints.NotNull;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
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
	public CsvParserSettings createCsvParserSettings() {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setFormat(createCsvFormat());
		settings.setHeaderExtractionEnabled(parseHeaders);
		settings.setMaxColumns(maxColumns);
		return settings;
	}

	/**
	 * Helper method to generate writer settings from the provided options in this class.
	 * @return Setting object that can be passed into a {@link CsvWriter}.
	 */
	public CsvWriterSettings createCsvWriterSettings() {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setMaxColumns(maxColumns);
		settings.setFormat(createCsvFormat());
		return settings;
	}
	
	/**
	 * Helper method to generate format settings from the provided options in this class.
	 * @return Format object that can be passed into {@link CsvWriterSettings} and {@link CsvParserSettings}.
	 */
	public CsvFormat createCsvFormat() {
		CsvFormat format = new CsvFormat();
		format.setQuoteEscape(getEscape());
		format.setCharToEscapeQuoteEscaping(getEscape());
		format.setComment(getComment());
		format.setDelimiter(getDelimeter());
		format.setLineSeparator(getLineSeparator());
		format.setQuote(getQuote());
		return format;
	}
}