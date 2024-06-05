package com.bakdata.conquery.models.preproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

/**
 * Small state machine to encapsulate reading of Preprocessed-Data in correct order:
 * Header then Dictionaries then Data. Only this order is possible.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PreprocessedReader implements AutoCloseable, Iterator<PreprocessedData> {
	@Override
	public void close() throws IOException {
		parser.close();
	}

	@Accessors(fluent = true)
	@RequiredArgsConstructor
	public enum LastRead {
		DATA(null), HEADER(DATA), BEGIN(HEADER);

		@Getter
		private final LastRead next;
	}

	@Getter
	private LastRead lastRead = LastRead.BEGIN;
	private int bucketsRemaining;
	private final JsonParser parser;

	public PreprocessedReader(InputStream inputStream, ObjectMapper objectMapper) throws IOException {

		parser = objectMapper.copy().enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
							 .getFactory()
							 .createParser(inputStream);
	}


	public PreprocessedHeader readHeader() throws IOException {
		Preconditions.checkState(lastRead.equals(LastRead.BEGIN));

		final PreprocessedHeader header = parser.readValueAs(PreprocessedHeader.class);
		bucketsRemaining = header.getNumberOfBuckets();

		lastRead = lastRead.next();
		return header;
	}


	@Override
	public boolean hasNext() {
		return bucketsRemaining > 0;
	}

	@SneakyThrows
	@Override
	public PreprocessedData next() {
		bucketsRemaining--;

		return parser.readValueAs(PreprocessedData.class);
	}

}
