package com.bakdata.conquery.models.preproc;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Small state machine to encapsulate reading of Preprocessed-Data in correct order:
 * Header then Dictionaries then Data. Only this order is possible.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class PreprocessedReader implements AutoCloseable {
	@Override
	public void close() throws IOException {
		parser.close();
	}

	@Accessors(fluent = true)
	@RequiredArgsConstructor
	private enum LastRead {
		DATA(null), DICTIONARIES(DATA), HEADER(DICTIONARIES), BEGIN(HEADER);

		@Getter
		private final LastRead next;
	}

	private LastRead read = LastRead.BEGIN;
	private final JsonParser parser;

	public PreprocessedHeader readHeader() throws IOException {
		Preconditions.checkState(read.equals(LastRead.BEGIN));

		final PreprocessedHeader header = parser.readValueAs(PreprocessedHeader.class);

		read = read.next();
		return header;
	}

	public PreprocessedDictionaries readDictionaries() throws IOException {
		Preconditions.checkState(read.equals(LastRead.HEADER));

		final PreprocessedDictionaries dictionaries = parser.readValueAs(PreprocessedDictionaries.class);

		read = read.next();
		return dictionaries;
	}

	public PreprocessedData readData() throws IOException {
		Preconditions.checkState(read.equals(LastRead.DICTIONARIES));

		final PreprocessedData dictionaries = parser.readValueAs(PreprocessedData.class);

		read = read.next();
		return dictionaries;
	}

}
