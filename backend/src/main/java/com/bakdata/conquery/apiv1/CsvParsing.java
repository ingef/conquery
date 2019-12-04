package com.bakdata.conquery.apiv1;

import com.bakdata.conquery.models.config.ConqueryConfig;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvWriter;

public class CsvParsing {

	public static CsvParser createParser() {
		return new CsvParser(ConqueryConfig.getInstance().getCsv().createCsvParserSettings());
	}

	public static CsvWriter createWriter() {
		return new CsvWriter(ConqueryConfig.getInstance().getCsv().createCsvWriterSettings());
	}
}
