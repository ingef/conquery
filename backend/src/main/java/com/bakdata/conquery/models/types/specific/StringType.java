package com.bakdata.conquery.models.types.specific;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDictionary;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;

import lombok.Getter;
import lombok.Setter;

@CPSType(base = CType.class, id = "STRING")
public class StringType extends CType<Integer, StringType> implements IStringType {

	@Getter @Setter @NotNull @Nonnull
	private DictionaryId dictionaryId = new DictionaryId(new DatasetId("null"), UUID.randomUUID().toString());
	@JsonIgnore @Setter @Getter
	private transient Dictionary dictionary = new Dictionary(dictionaryId);

	public StringType() {
		super(MajorTypeId.STRING, int.class);
	}

	@Override
	public void init(DatasetId dataset) {
		dictionaryId = new DictionaryId(dataset, dictionaryId.getDictionary());
		dictionary.setDataset(dataset);
	}
	
	@Override
	protected Integer parseValue(String value) throws ParsingException {
		return dictionary.add(value);
	}


	@Override
	public String createScriptValue(Integer value) {
		return dictionary.getElement(value);
	}

	@Override
	public void writeHeader(OutputStream out) throws IOException {
		dictionary.tryCompress();
		Jackson.BINARY_MAPPER.writeValue(out, dictionary);
	}

	@Override
	public void readHeader(JsonParser input) throws IOException {
		dictionary = Jackson.BINARY_MAPPER.readValue(input, Dictionary.class);
	}

	@Override
	public void storeExternalInfos(NamespacedStorage storage, Consumer<WorkerMessage> messageConsumer) {
		dictionary.setName(dictionaryId.getDictionary());
		dictionary.setDataset(dictionaryId.getDataset());
		messageConsumer.accept(new UpdateDictionary(dictionary));
	}

	@Override
	public void loadExternalInfos(NamespacedStorage storage) {
		dictionary = Objects.requireNonNull(storage.getDictionary(dictionaryId));
	}

	@Override
	public CType<?, StringType> bestSubType() {
		dictionary.tryCompress();
		if(dictionary.size() == 0) {
			return this;
		}

		EnumSet<StringTypeEncoded.Encoding> bases = EnumSet.allOf(StringTypeEncoded.Encoding.class);

		for (String value : dictionary) {
			bases.removeIf(encoding -> !encoding.canDecode(value));
			if(bases.isEmpty())
				return this;
		}

		if (!bases.isEmpty()) {
			return new StringTypeEncoded(
					bases.stream()
						.min(StringTypeEncoded.Encoding::compareTo)
						.orElseThrow(() -> new IllegalStateException("Bases not empty, but no valid minimum.")),
					getLines(),
					getNullLines()
			);
		}

		return this;
	}

	@Override
	public boolean canStoreNull() {
		return true; //dictionaries can store null as -1
	}

	@Override
	public int size() {
		return dictionary.size();
	}
}
