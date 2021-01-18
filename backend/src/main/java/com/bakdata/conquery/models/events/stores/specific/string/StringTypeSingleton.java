package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.Collections;
import java.util.Iterator;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.events.stores.base.BooleanStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.Iterators;
import lombok.Getter;
import lombok.Setter;

/**
 * Only one string.
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_SINGLETON")
public class StringTypeSingleton extends StringType {

	private final String singleValue;
	private final BooleanStore delegate;

	@JsonCreator
	public StringTypeSingleton(String singleValue, BooleanStore delegate) {
		super();
		this.singleValue = singleValue;
		this.delegate = delegate;
	}

	@Override
	public void setIndexStore(ColumnStore<Long> indexStore) {

	}

	@Override
	public int size() {
		return singleValue == null ? 0 : 1;
	}

	@Override
	public StringTypeSingleton select(int[] starts, int[] length) {
		return new StringTypeSingleton(singleValue, delegate.select(starts, length));
	}

	@Override
	public String getElement(int id) {
		return singleValue;
	}

	@Override
	public String createScriptValue(Integer value) {
		return singleValue;
	}

	@Override
	public int getId(String value) {
		if (value != null && value.equals(singleValue)) {
			return 0;
		}
		return -1;
	}

	@Override
	public Iterator<String> iterator() {
		if (singleValue == null) {
			return Collections.emptyIterator();
		}
		return Iterators.singletonIterator(singleValue);
	}

	@Override
	public long estimateEventBits() {
		return Byte.SIZE;
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {

	}

	@Override
	public void set(int event, Integer value) {
		getDelegate().set(event, value != null && value == 0);
	}

	@Override
	public boolean has(int event) {
		return getDelegate().getBoolean(event);
	}

	@Override
	public Integer get(int event) {
		return getString(event);
	}

	@Override
	public int getString(int event) {
		return 0;
	}
}
