package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.View;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

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
public class DictionaryStore implements ColumnStore {

	protected IntegerStore numberType;

	@NsIdRef
	private Dictionary dictionary;

	@JsonView(View.Persistence.Manager.class)
	private Set<Integer> usedEntries;

	public DictionaryStore(IntegerStore store, Dictionary dictionary) {
		this.numberType = store;
		this.dictionary = dictionary;
	}


	public Stream<byte[]> iterateStrings() {
		Preconditions.checkState(usedEntries != null, "usedEntries are not set yet.");

		return usedEntries.stream().map(dictionary::getElement);
	}

	@Override
	public DictionaryStore createDescription() {
		DictionaryStore result = new DictionaryStore(numberType.createDescription(), dictionary);

		result.setUsedEntries(collectUsedStrings(this));

		return result;
	}

	@NotNull
	private static Set<Integer> collectUsedStrings(DictionaryStore stringStore) {
		Set<Integer> sampled = new HashSet<>();
		for (int event = 0; event < stringStore.getLines(); event++) {
			if (!stringStore.has(event)) {
				continue;
			}

			sampled.add(stringStore.getString(event));
		}
		return sampled;
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

	public int getString(int event) {
		return (int) getNumberType().getInteger(event);
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
	public DictionaryStore select(int[] starts, int[] length) {
		return new DictionaryStore(numberType.select(starts, length), dictionary);
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
