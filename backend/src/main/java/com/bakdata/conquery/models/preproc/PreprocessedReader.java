package com.bakdata.conquery.models.preproc;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.InjectingCentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.worker.SingletonNamespaceCollection;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
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
		DATA(null), DICTIONARIES(DATA), HEADER(DICTIONARIES), BEGIN(HEADER);

		@Getter
		private final LastRead next;
	}

	@Getter
	private LastRead lastRead = LastRead.BEGIN;
	private final JsonParser parser;
	private final Map<Id<?>, Identifiable<?>> replacements = new HashMap<>();

	public PreprocessedReader(InputStream inputStream, ObjectMapper objectMapper) throws IOException {
		final InjectingCentralRegistry injectingCentralRegistry = new InjectingCentralRegistry(replacements);
		final SingletonNamespaceCollection namespaceCollection = new SingletonNamespaceCollection(injectingCentralRegistry);

		parser = namespaceCollection.injectIntoNew(objectMapper)
				.enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET)
				.getFactory()
				.createParser(inputStream);
	}

	public void addReplacement(Id<?> id, Identifiable<?> replacement) {
		this.replacements.put(id, replacement);
	}

	public <K extends Id<?>, V extends Identifiable<?>> void addAllReplacements(Map<K, V> replacements) {
		this.replacements.putAll(replacements);
	}

	public PreprocessedHeader readHeader() throws IOException {
		Preconditions.checkState(lastRead.equals(LastRead.BEGIN));

		final PreprocessedHeader header = parser.readValueAs(PreprocessedHeader.class);

		lastRead = lastRead.next();
		return header;
	}

	public PreprocessedDictionaries readDictionaries() throws IOException {
		Preconditions.checkState(lastRead.equals(LastRead.HEADER));

		final PreprocessedDictionaries dictionaries = parser.readValueAs(PreprocessedDictionaries.class);

		lastRead = lastRead.next();
		return dictionaries;
	}

	public PreprocessedData readData() throws IOException {
		Preconditions.checkState(lastRead.equals(LastRead.DICTIONARIES));

		final PreprocessedData dictionaries = parser.readValueAs(PreprocessedData.class);

		lastRead = lastRead.next();
		return dictionaries;
	}

}
