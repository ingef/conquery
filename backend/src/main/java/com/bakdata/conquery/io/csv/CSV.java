package com.bakdata.conquery.io.csv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.models.config.CSVConfig;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;


public class CSV implements Closeable {
	
	private final CsvParserSettings settings;
	private final CSVConfig config;
	private BufferedReader reader;

	public CSV(CSVConfig config, File file) throws IOException {
		this(
			config, 
			isGZipped(file) ?
				new GZIPInputStream(new FileInputStream(file))
			:
				new FileInputStream(file)
		);
	}
	
	public static boolean isGZipped(File file) {
		return file.getName().endsWith(".gz");
	}

	public CSV(CSVConfig config, InputStream input) throws IOException {
		this.config = config;
		CsvFormat format = new CsvFormat();
		{
			format.setQuoteEscape(config.getEscape());
			format.setCharToEscapeQuoteEscaping(config.getEscape());
			format.setComment(config.getComment());
			format.setDelimiter(config.getDelimeter());
			format.setLineSeparator(config.getLineSeparator());
			format.setQuote(config.getQuote());
		}
		settings = new CsvParserSettings();
		{
			settings.setFormat(format);
		}
		
		reader = new BufferedReader(new InputStreamReader(input, config.getEncoding()));
	}

	public static Stream<String[]> streamContent(CSVConfig config, File file) throws IOException {
		CSV csv = new CSV(config, file);
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(csv.iterateContent(), Spliterator.ORDERED),
			false
		)
		.onClose(csv::closeUnchecked);
	}
	
	public static Stream<String[]> streamContent(CSVConfig config, InputStream input) throws IOException {
		CSV csv = new CSV(config, input);
		return StreamSupport.stream(
			Spliterators.spliteratorUnknownSize(csv.iterateContent(), Spliterator.ORDERED),
			false
		)
		.onClose(csv::closeUnchecked);
	}

	public Iterator<String[]> iterateContent() throws IOException {
		Iterator<String[]> it = new AsyncIterator<>(
			new CsvParser(settings)
				.iterate(reader)
				.iterator()
		);
		
		//skip the header line
		if(config.isSkipHeader() && it.hasNext()) {
			it.next();
		}
		
		return it;
	}
	
	@Override
	public void close() throws IOException {
		reader.close();
	}

	private void closeUnchecked() {
		try {
			reader.close();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
