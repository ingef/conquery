package com.bakdata.conquery.models.events.stores.specific.string;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Strings are stored in a Dictionary, ids are handles into the Dictionary.
 *
 * @implNote this is NOT a {@link StringStore}, but is the base class of it. This enables some shenanigans with encodings.
 */
@Getter
@Setter
@Slf4j
@CPSType(base = ColumnStore.class, id = "STRING_DICTIONARY")
@NoArgsConstructor
public class StringTypeDictionary implements ColumnStore {

	protected IntegerStore numberType;

	@NsIdRef
	private Dictionary dictionary;

	public StringTypeDictionary(IntegerStore store, Dictionary dictionary) {
		this.numberType = store;
		this.dictionary = dictionary;
	}


	@Override
	public int getLines() {
		return numberType.getLines();
	}

	public byte[] getElement(int value) {
		return dictionary.getElement(value);
	}

	@Override
	public Object createScriptValue(int event) {
		return getElement(getString(event));
	}


	public int size() {
		return dictionary.size();
	}

	public int getId(byte[] value) {
		return dictionary.getId(value);
	}

	@Override
	public String toString() {
		return "StringTypeDictionary(dictionary=" + dictionary + ", numberType=" + numberType + ")";
	}

	@Override
	public long estimateTypeSizeBytes() {
		return dictionary.estimateMemoryConsumption();
	}


	@Override
	public StringTypeDictionary select(int[] starts, int[] length) {
		return new StringTypeDictionary(numberType.select(starts, length), dictionary);
	}

	public int getString(int event) {
		return (int) getNumberType().getInteger(event);
	}

	@Override
	public long estimateEventBits() {
		return numberType.estimateEventBits();
	}

	public void set(int event, int value) {
		numberType.setInteger(event, value);
	}

	@Override
	public void setNull(int event) {
		numberType.setNull(event);
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}

	public void setIndexStore(IntegerStore newType) {
		numberType = newType;
	}
}
