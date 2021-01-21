package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Strings are stored in a Dictionary, ids are handles into the Dictionary.
 *
 * @implNote this is NOT a {@link StringType}, but is the base class of it. This enables some shenanigans with encodings.
 */
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
		this.numberType = numberType;
		this.dictionary = dictionary;
		this.name = name;
	}

	@JsonCreator
	public StringTypeDictionary(ColumnStore<Long> numberType, DatasetId dataset, String name) {
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
	public long estimateTypeSizeBytes() {
		return dictionary.estimateMemoryConsumption();
	}


	public void setUnderlyingDictionary(DictionaryId newDict) {
		name = newDict.getDictionary();
		this.dataset = Objects.requireNonNull(newDict.getDataset());
	}

	@Override
	public StringTypeDictionary select(int[] starts, int[] length) {
		return new StringTypeDictionary(numberType.select(starts, length), getDataset(), getName());
	}

	@Override
	public int getString(int event) {
		return (int) getNumberType().getInteger(event);
	}

	@Override
	public Integer get(int event) {
		return getString(event);
	}

	@Override
	public long estimateEventBits() {
		return numberType.estimateEventBits();
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
