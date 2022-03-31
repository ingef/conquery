package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.Iterator;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.Encoding;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * Compacted String store, that uses two methods to reduce memory footprint:
 *  1. Use a byte efficient encoding string for the actual string. See {@link Encoding}
 *  2. Store the byte string in an appropriate data structure. See {{@link Dictionary and sub classes}}
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_ENCODED")
public class StringTypeEncoded implements StringStore {

	@Nonnull
	protected StringTypeDictionary subType;
	@NonNull
	private Encoding encoding;

	/**
	 * Cache element lookups and as they might be time consuming, when a trie traversal is necessary (See {@link com.bakdata.conquery.util.dict.SuccinctTrie}).
	 */
	@JsonIgnore
	private final LoadingCache<Integer,String> elementCache;

	@JsonCreator
	public StringTypeEncoded(StringTypeDictionary subType, Encoding encoding) {
		super();
		this.subType = subType;
		this.encoding = encoding;
		elementCache = CacheBuilder.newBuilder()
				.softValues()
				.build(new CacheLoader<Integer, String>() {
					@Override
					@ParametersAreNonnullByDefault
					public String load(Integer key) throws Exception {
						return subType.getElement(key);
					}
				});
	}

	@Override
	@SneakyThrows
	public String getElement(int value) {
		return elementCache.get(value);
	}

	@Override
	public int getLines() {
		return subType.getLines();
	}

	@Override
	public String createScriptValue(int event) {
		return getElement(getString(event));
	}


	@Override
	public int size() {
		return subType.size();
	}

	@Override
	public int getId(String value) {
		// Make sure we can even decode before doing so
		if (!encoding.canEncode(value)) {
			return -1;
		}

		return subType.getId(value);
	}

	@Override
	public Iterator<String> iterator() {
		Iterator<String> subIt = subType.iterator();
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return subIt.hasNext();
			}

			@Override
			public String next() {
				return subIt.next();
			}
		};
	}

	@Override
	public String toString() {
		return "StringTypeEncoded(encoding=" + encoding + ", subType=" + subType + ")";
	}

	@Override
	public long estimateEventBits() {
		return subType.estimateEventBits();
	}

	@Override
	public long estimateMemoryConsumptionBytes() {
		return subType.estimateMemoryConsumptionBytes();
	}

	@Override
	public long estimateTypeSizeBytes() {
		return subType.estimateTypeSizeBytes();
	}


	@Override
	public Dictionary getUnderlyingDictionary() {
		return subType.getDictionary();
	}

	@Override
	public boolean isDictionaryHolding() {
		return true;
	}

	@Override
	public void setIndexStore(IntegerStore newType) {
		subType.setIndexStore(newType);
	}

	@Override
	public StringTypeEncoded select(int[] starts, int[] length) {
		return new StringTypeEncoded(subType.select(starts, length), getEncoding());
	}

	@Override
	public void setString(int event, int value) {
		subType.set(event, value);
	}

	@Override
	public void setNull(int event) {
		subType.setNull(event);
	}

	@Override
	public int getString(int event) {
		return subType.getString(event);
	}

	@Override
	public boolean has(int event) {
		return subType.has(event);
	}

}
