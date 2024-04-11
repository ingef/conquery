package com.bakdata.conquery.models.preproc;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
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
	public enum LastRead {
		DATA(null), HEADER(DATA), BEGIN(HEADER);

		@Getter
		private final LastRead next;
	}

	@Getter
	private LastRead lastRead = LastRead.BEGIN;
	private final JsonParser parser;

	public PreprocessedReader(InputStream inputStream, ObjectMapper objectMapper) throws IOException {

		parser = objectMapper.copy().enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
							 .getFactory()
							 .createParser(inputStream);
	}


	public PreprocessedHeader readHeader() throws IOException {
		Preconditions.checkState(lastRead.equals(LastRead.BEGIN));

		final PreprocessedHeader header = parser.readValueAs(PreprocessedHeader.class);

		lastRead = lastRead.next();
		return header;
	}



	public PreprocessedData readData() throws IOException {
		if(parser.isClosed()){
			//TODO better handling obviously.
			return null;
		}

		try {
			final PreprocessedData dictionaries = parser.readValueAs(PreprocessedData.class);
			return dictionaries;
		}catch (MismatchedInputException exception){
			// MismatchedInputException is thrown when EOF file is reached.
			//TODO actually handle end of input without state parameter.
			return null;
		}
	}

}
