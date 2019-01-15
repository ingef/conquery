package com.bakdata.conquery.apiv1;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

import com.github.powerlibraries.io.In;
import com.google.common.collect.AbstractIterator;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import lombok.AllArgsConstructor;

public class CSVReader {

	public static Iterable<String[]> readRaw(File inputFile) throws IOException {
		return readRaw(In.file(inputFile).withUTF8().asReader());
	}

	public static Iterable<String[]> readRaw(Reader r) {
		return new Iterable<String[]>() {
			@Override
			public Iterator<String[]> iterator() {
				CsvParser parser = createParser();
				parser.beginParsing(r);
				return new CsvParserIterator(parser);
			}
		};
	}

	public static Iterator<String[]> iterate(CsvParser parser) {
		return new CsvParserIterator(parser);
	}

	public static CsvParser createParser() {
		CsvParserSettings settings = new CsvParserSettings();
		configureFormat(settings.getFormat());
		settings.setColumnReorderingEnabled(false);
		settings.setMaxCharsPerColumn(-1);
		return new CsvParser(settings);
	}

	public static void configureFormat(CsvFormat format) {
		format.setDelimiter(';');
		format.setLineSeparator("\n");
		format.setCharToEscapeQuoteEscaping('\\');
		format.setQuoteEscape('\\');
	}

	@AllArgsConstructor
	private static class CsvParserIterator extends AbstractIterator<String[]> {

		private CsvParser parser;

		@Override
		protected String[] computeNext() {
			String[] r = parser.parseNext();
			if (r == null) {
				return endOfData();
			} else {
				return r;
			}
		}
	}
}
