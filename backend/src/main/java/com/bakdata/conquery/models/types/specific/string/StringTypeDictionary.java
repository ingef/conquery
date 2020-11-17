package com.bakdata.conquery.models.types.specific.string;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.DictionaryEntry;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.specific.VarIntType;
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
public class StringTypeDictionary extends CTypeVarInt<Integer> {

	@JsonIgnore
	private transient Dictionary dictionary;

	// todo use NsIdRef
	private String name;

	@InternalOnly
	private DatasetId dataset;

	public StringTypeDictionary(VarIntType numberType, Dictionary dictionary, String name) {
		super(MajorTypeId.STRING, numberType);
		this.dictionary = dictionary;
		this.name = name;
	}

	@JsonCreator
	public StringTypeDictionary(VarIntType numberType, DatasetId dataset, String name) {
		super(MajorTypeId.STRING, numberType);
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

	@Override
	public void storeExternalInfos(Consumer<Dictionary> dictionaryConsumer) {
		dictionaryConsumer.accept(dictionary);
	}

	@Override
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
}
