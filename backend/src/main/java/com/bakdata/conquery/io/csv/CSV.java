package com.bakdata.conquery.io.csv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import com.bakdata.conquery.models.config.CSVConfig;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;


public class CSV implements Closeable {
	
	private final CsvParserSettings settings;
	private final CSVConfig config;
	private BufferedReader reader;
	private List<AsyncIterator<String[]>> iterators = new ArrayList<>();

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
		settings = config.createCsvParserSettings();
		
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

	public AsyncIterator<String[]> iterateContent() throws IOException {
		AsyncIterator<String[]> it = new AsyncIterator<>(
			new CsvParser(settings)
				.iterate(reader)
				.iterator()
		);
		
		//skip the header line
		if(config.isSkipHeader() && it.hasNext()) {
			it.next();
		}
		iterators.add(it);
		
		return it;
	}
	
	@Override
	public void close() throws IOException {
		reader.close();
		for(AsyncIterator<?> it:iterators) {
			it.close();
		}
	}

	private void closeUnchecked() {
		try {
			reader.close();
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
		try {
			for(AsyncIterator<?> it:iterators) {
				it.close();
			}
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
}
