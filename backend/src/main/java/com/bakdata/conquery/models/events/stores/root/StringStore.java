package com.bakdata.conquery.models.events.stores.root;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * {@link ColumnStore} for dictionary encoded string values.
 *
 * See also {@link MajorTypeId#STRING} and {@link com.bakdata.conquery.models.preproc.parser.specific.StringParser}.
 *
 * This class has a lot of special methods for handling dictionary encoded values.
 *
 * @implSpec Every implementation must guarantee IDs between 0 and size.
 *
 */
public interface StringStore extends ColumnStore {


	int getString(int event);
	void setString(int event, int value);

	String getElement(int id);

	/**
	 * Number of distinct values in this Store.
	 */
	int size();

	/**
	 * TODO documentation of this class!
	 */
	@RequiredArgsConstructor
	@Getter
	static class StringStoreDescription implements StringStore {
		private final Set<Integer> indices;
		private final StringStore actual;

		@Override
		public boolean has(int event) {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public Object createScriptValue(int event) {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public long estimateEventBits() {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public int getLines() {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public <T extends ColumnStore> T select(int[] starts, int[] lengths) {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public void setNull(int event) {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public int getString(int event) {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public void setString(int event, int value) {
			throw new IllegalStateException("Should not be evaluated.");
		}

		@Override
		public String getElement(int id) {
			return actual.getElement(id);
		}

		@Override
		public int size() {
			return getIndices().size();
		}

		@NotNull
		public Stream<String> streamValues() {
			return indices.stream().map(actual::getElement);
		}

		@Override
		public int getId(String value) {
			return actual.getId(value);
		}

		@Override
		public Dictionary getUnderlyingDictionary() {
			return actual.getUnderlyingDictionary();
		}

		@Override
		public void setUnderlyingDictionary(Dictionary dictionary) {
			actual.setUnderlyingDictionary(dictionary);
		}

		@Override
		public boolean isDictionaryHolding() {
			return actual.isDictionaryHolding();
		}

		@Override
		public void setIndexStore(IntegerStore newType) {
			throw new IllegalStateException("Should not be evaluated.");
		}
	}

	@Override
	default ColumnStore createDescription() {
		Set<Integer> actual = new HashSet<>();
		for (int event = 0; event < getLines(); event++) {
			if (!has(event)){
				continue;
			}

			actual.add(getString(event));
		}

		ColumnStore description = ColumnStore.super.createDescription();

		return new StringStoreDescription(actual, ((StringStore) description));


	}

	/**
	 * Lookup the id of a value in the dictionary.
	 */
	int getId(String value);


	@JsonIgnore
	Dictionary getUnderlyingDictionary();
	@JsonIgnore
	void setUnderlyingDictionary(Dictionary dictionary);

	@JsonIgnore
	boolean isDictionaryHolding();



	/**
	 * After applying DictionaryMapping a new store might be needed.
	 */
	void setIndexStore(IntegerStore newType);

}
