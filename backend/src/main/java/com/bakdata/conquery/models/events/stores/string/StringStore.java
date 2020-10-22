package com.bakdata.conquery.models.events.stores.string;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.ColumnStore;
import com.bakdata.conquery.models.events.stores.ColumnStoreAdapter;
import com.bakdata.conquery.models.events.stores.base.IntegerStore;
import com.bakdata.conquery.models.types.specific.StringTypeEncoded;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
@CPSType(id = "STRINGS", base = ColumnStore.class)
public class StringStore extends ColumnStoreAdapter<Integer> {

	private final ColumnStore<Long> store;
	private final StringTypeEncoded.Encoding encoding;
	private final Dictionary dictionary;


	@JsonCreator
	public StringStore(ColumnStore<Long> store, StringTypeEncoded.Encoding encoding, Dictionary dictionary) {
		this.store = store;
		this.encoding = encoding;
		this.dictionary = dictionary;
	}

	public static StringStore create(int size, StringTypeEncoded.Encoding encoding, Dictionary dictionary) {
		return new StringStore(IntegerStore.create(size), encoding, dictionary);
	}

	@Override
	public Object getAsObject(int event) {
		return getString(event);
	}

	public Integer get(int event) {
		return (int) store.getInteger(event);
	}

	public StringStore select(int[] starts, int[] ends) {
		return new StringStore(store.select(starts, ends), encoding, dictionary);
	}

	@Override
	public void set(int event, Integer value) {
		if (value == null) {
			store.set(event, null);
			return;
		}
		store.set(event, value.longValue());
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}
