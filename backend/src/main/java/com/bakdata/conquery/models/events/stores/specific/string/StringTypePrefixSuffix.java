package com.bakdata.conquery.models.events.stores.specific.string;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
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
public class StringTypePrefixSuffix implements StringStore {

	@Nonnull
	protected StringStore subType;

	@NonNull
	private String prefix;

	@NonNull
	private String suffix;

	@JsonCreator
	public StringTypePrefixSuffix(StringStore subType, String prefix, String suffix) {
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
	public int getLines() {
		return subType.getLines();
	}

	@Override
	public String createScriptValue(int event) {
		return prefix + subType.createScriptValue(event) + suffix;
	}

	@Override
	public int getId(String value) {
		if (value.startsWith(prefix)) {
			return subType.getId(value.substring(prefix.length()));
		}
		return -1;
	}

	@Override
	public void setIndexStore(IntegerStore indexStore) {
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
				return getPrefix() + subIt.next() + getSuffix();
			}
		};
	}


	@Override
	public StringTypePrefixSuffix select(int[] starts, int[] length) {
		return new StringTypePrefixSuffix(subType.select(starts, length), getPrefix(), getSuffix());
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
	public void setUnderlyingDictionary(Dictionary dictionary) {
		subType.setUnderlyingDictionary(dictionary);
	}

	@Override
	public boolean isDictionaryHolding() {
		return subType.isDictionaryHolding();
	}

	@Override
	public int getString(int event) {
		return subType.getString(event);
	}

	@Override
	public void setString(int event, int value) {
		subType.setString(event, value);
	}

	@Override
	public void setNull(int event) {
		subType.setNull(event);
	}

	@Override
	public boolean has(int event) {
		return subType.has(event);
	}
}
