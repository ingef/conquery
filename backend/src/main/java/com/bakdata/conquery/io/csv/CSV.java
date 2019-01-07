package com.bakdata.conquery.io.csv;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.slf4j.Logger;

import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.util.io.ProgressBar;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;


public class CSV implements Closeable {
	
	private final static ProgressBar PROGRESS_BAR = new ProgressBar(0, System.out);
	
	private final CsvParserSettings settings;
	private final CSVConfig config;
	private BufferedReader reader;
	private CountingInputStream counter;
	private final long totalSizeToRead;
	private long read = 0;
	
	public CSV(CSVConfig config, File file) throws IOException {
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
		
		totalSizeToRead = file.length();
		counter = new CountingInputStream(new FileInputStream(file));
		
		InputStream in = file.getName().endsWith(".gz")?new GZIPInputStream(counter):counter;
			
		reader = new BufferedReader(new InputStreamReader(in, config.getEncoding()));
	}

	public Iterator<String[]> iterateContent(String name, Logger log) throws IOException {
		Iterator<String[]> it = new CsvParser(settings)
				.iterate(reader)
				.iterator();
		
		//skip the header line
		if(config.isSkipHeader() && it.hasNext()) {
			it.next();
		}
		
		if(totalSizeToRead > 1024*1024) {
			PROGRESS_BAR.addMaxValue(totalSizeToRead);
			return new Iterator<String[]>() {
				
				@Override
				public String[] next() {
					long newRead = counter.getByteCount();
					PROGRESS_BAR.addCurrentValue(newRead - read);
					read = newRead;
					return it.next();
				}
				
				@Override
				public boolean hasNext() {
					return it.hasNext();
				}
			};
		}
		else {
			return it;
		}
	}
	
	@Override
	public void close() throws IOException {
		reader.close();
	}
}
