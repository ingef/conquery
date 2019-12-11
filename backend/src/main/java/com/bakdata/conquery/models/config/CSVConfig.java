package com.bakdata.conquery.models.config;

import javax.validation.constraints.NotNull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParserSettings;
import com.univocity.parsers.csv.CsvWriterSettings;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.hibernate.validator.constraints.Length;

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
	 * Script used to generate the CSV column names from CQConcept and Select information.
	 * The script has an instance of SelectResultInfo named columnInfo available to construct the name.
	 */
	@ValidColumnNamer
	private String columnNamerScript = "java.lang.String.format(\"%s %s %s\",columnInfo.getSelect().getHolder().findConcept().getLabel(), columnInfo.getCqConcept().getLabel(),columnInfo.getSelect().getLabel())";
	
	public CsvParserSettings createCsvParserSettings() {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setFormat(createCsvFormat());
		settings.setHeaderExtractionEnabled(parseHeaders);
		settings.setMaxColumns(maxColumns);
		return settings;
	}
	
	public CsvWriterSettings createCsvWriterSettings() {
		CsvWriterSettings settings = new CsvWriterSettings();
		settings.setMaxColumns(maxColumns);
		settings.setFormat(createCsvFormat());
		return settings;
	}

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