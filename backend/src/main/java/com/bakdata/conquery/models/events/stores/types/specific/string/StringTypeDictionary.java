package com.bakdata.conquery.models.events.stores.types.specific.string;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.bakdata.conquery.models.events.stores.types.ColumnStore;
import com.bakdata.conquery.models.events.stores.types.MajorTypeId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@CPSType(base = ColumnStore.class, id = "STRING_DICTIONARY")
public class StringTypeDictionary extends ColumnStore<Integer> {

	protected ColumnStore<Long> numberType;

	@JsonIgnore
	private transient Dictionary dictionary;

	// todo use NsIdRef
	private String name;

	@InternalOnly
	private DatasetId dataset;

	public StringTypeDictionary(ColumnStore<Long> numberType, Dictionary dictionary, String name) {
		super(MajorTypeId.STRING);
		this.numberType = numberType;
		this.dictionary = dictionary;
		this.name = name;
	}

	@JsonCreator
	public StringTypeDictionary(ColumnStore<Long> numberType, DatasetId dataset, String name) {
		super(MajorTypeId.STRING);
		this.numberType = numberType;
		this.name = name;
		this.dataset = dataset;
	}

	@Override
	public Object createScriptValue(Integer value) {
		return getElement(value);
	}

	public byte[] getElement(int value) {
		return dictionary.getElement(value);
	}

	@Override
	public Object createPrintValue(Integer value) {
		return getElement(value);
	}


	public void loadDictionaries(NamespacedStorage storage) {
		// todo consider implementing this with Id-Injection instead of hand-wiring.
		final DictionaryId dictionaryId = new DictionaryId(getDataset(), getName());
		log.trace("Loading Dictionary[{}]", dictionaryId);
		dictionary = Objects.requireNonNull(storage.getDictionary(dictionaryId));
	}

	public int size() {
		return dictionary.size();
	}

	public int getId(byte[] value) {
		return dictionary.getId(value);
	}

	public Iterator<byte[]> iterator() {
		if(dictionary == null){
			return Collections.emptyIterator();
		}

		return Iterators.transform(dictionary.iterator(), DictionaryEntry::getValue);
	}

	@Override
	public String toString() {
		return "StringTypeDictionary[dictionary=" + dictionary + ", numberType=" + numberType + "]";
	}

	@Override
	public long estimateTypeSize() {
		return dictionary.estimateMemoryConsumption();
	}


	public void setUnderlyingDictionary(DictionaryId newDict) {
		name = newDict.getDictionary();
		this.dataset = newDict.getDataset();
	}

	@Override
	public StringTypeDictionary select(int[] starts, int[] length) {
		return new StringTypeDictionary(numberType.select(starts, length), getDataset(), getName());
	}

	@Override
	public Integer get(int event) {
		return getNumberType().get(event).intValue();
	}

	@Override
	public long estimateEventBytes() {
		return numberType.estimateEventBytes();
	}

	@Override
	public void set(int event, Integer value) {
		if (value == null) {
			numberType.set(event, null);
		}
		else {
			numberType.set(event, value.longValue());
		}
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}

	public void setIndexStore(ColumnStore<Long> newType) {
		numberType = newType;
	}
}
