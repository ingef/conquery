package com.bakdata.conquery.apiv1;

import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class CsvParsing {

	public static CsvParser createParser() {
		CsvParserSettings settings = new CsvParserSettings();
		CsvFormat format = settings.getFormat();
		//TODO why is this not configured?
		format.setDelimiter(';');
		format.setLineSeparator("\n");
		format.setCharToEscapeQuoteEscaping('\\');
		format.setQuoteEscape('\\');
		settings.setColumnReorderingEnabled(false);
		settings.setMaxCharsPerColumn(-1);
		return new CsvParser(settings);
	}
}
