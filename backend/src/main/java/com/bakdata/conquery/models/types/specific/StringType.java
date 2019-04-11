package com.bakdata.conquery.models.types.specific;

import java.io.IOException;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
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
	protected Integer parseValue(String value) {
		return dictionary.add(value);
	}


	@Override @Deprecated
	public String createScriptValue(Integer value) {
		return createScriptValue(value.intValue());
	}
	
	@Override
	public String createScriptValue(int value) {
		return dictionary.getElement(value);
	}

	@Override
	public int getStringId(String string) {
		return dictionary.getId(string);
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
	public void storeExternalInfos(NamespacedStorage storage, Consumer<Dictionary> dictionaryConsumer) {
		dictionary.setName(dictionaryId.getDictionary());
		dictionary.setDataset(dictionaryId.getDataset());
		dictionaryConsumer.accept(dictionary);
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
	
	@Override
	public Iterator<String> iterator() {
		return dictionary.iterator();
	}
}
