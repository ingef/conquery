package com.bakdata.conquery.models.events.stores.specific.string;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

/**
 * Strings with common, but stripped prefix/suffix.
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_PREFIX")
@ToString(of = {"prefix", "suffix", "subType"})
public class StringTypePrefixSuffix extends StringType {

	@Nonnull
	protected StringType subType;

	@NonNull
	private String prefix;

	@NonNull
	private String suffix;

	@JsonCreator
	public StringTypePrefixSuffix(StringType subType, String prefix, String suffix) {
		super();
		this.subType = subType;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	@Override
	public String getElement(int value) {
		return prefix + subType.getElement(value) + suffix;
	}

	@Override
	public String createScriptValue(Integer value) {
		return prefix + subType.createScriptValue(value);
	}

	@Override
	public int getId(String value) {
		if (value.startsWith(prefix)) {
			return subType.getId(value.substring(prefix.length()));
		}
		return -1;
	}

	@Override
	public void setIndexStore(ColumnStore<Long> indexStore) {
		subType.setIndexStore(indexStore);
	}

	@Override
	public Iterator<String> iterator() {
		Iterator<String> subIt = subType.iterator();
		return new Iterator<String>() {
			@Override
			public boolean hasNext() {
				return subIt.hasNext();
			}

			@Override
			public String next() {
				return prefix + subIt.next();
			}
		};
	}


	@Override
	public StringTypePrefixSuffix doSelect(int[] starts, int[] length) {
		return new StringTypePrefixSuffix(subType.doSelect(starts, length), getPrefix(), getSuffix());
	}

	@Override
	public void loadDictionaries(NamespacedStorage storage) {
		subType.loadDictionaries(storage);
	}

	@Override
	public int size() {
		return subType.size();
	}

	@Override
	public long estimateEventBits() {
		return subType.estimateEventBits();
	}

	@Override
	public long estimateMemoryConsumptionBytes() {
		return (long) prefix.getBytes(StandardCharsets.UTF_8).length * Byte.SIZE +
			   (long) suffix.getBytes(StandardCharsets.UTF_8).length * Byte.SIZE +
			   subType.estimateMemoryConsumptionBytes();
	}

	@Override
	public long estimateTypeSizeBytes() {
		return subType.estimateTypeSizeBytes();
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return subType.getUnderlyingDictionary();
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {
		subType.setUnderlyingDictionary(newDict);
	}

	@Override
	public Integer get(int event) {
		return getString(event);
	}

	@Override
	public int getString(int event) {
		return subType.getString(event);
	}

	@Override
	public void set(int event, Integer value) {
		subType.set(event, value);
	}

	@Override
	public boolean has(int event) {
		return subType.has(event);
	}
}
